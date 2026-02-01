package encube.assignment.modules.notifications.service;

import com.google.protobuf.Message;
import encube.assignment.domain.FileState;
import encube.assignment.events.EventDeserializer;
import encube.assignment.events.FileDescriptorChangedEvent;
import encube.assignment.events.WebSocketConnectionChangedEvent;
import encube.assignment.events.WebSocketMessageReceivedEvent;
import encube.assignment.modules.notifications.domain.Subscription;
import encube.assignment.modules.notifications.domain.SubscriptionMessage;
import encube.assignment.modules.notifications.repository.SubscriptionRepository;
import encube.assignment.modules.websocket.service.WebSocketMessage;
import encube.assignment.modules.websocket.service.WebSocketService;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * <p>
 * The <code>NotificationService</code> sends notifications to users via WebSocket when users upload files to a canvas
 * they are subscribed to.  It listens to relevant events from Kafka and processes them accordingly.
 * </p>
 *
 * <p>
 * This is a good example of the functional scalability of this architecture. This service can be scaled independently,
 * it reacts to events only which are served by the platform and then calls another isolated function to push the message
 * to the end user. It is a feature added "on top" of generic functions and is not entangled with other business logic.
 * </p>
 */
@Service
@Slf4j
public class NotificationService implements ApplicationRunner {

    private final String kafkaBootstrapServers;

    private final JsonMapper jsonMapper;

    private final SubscriptionRepository subscriptionRepository;

    private final TransactionalOperator tx;
    private final WebSocketService webSocketService;

    public NotificationService(@Value("${kafka.bootstrap-servers}") String kafkaBootstrapServers,
                               JsonMapper jsonMapper,
                               SubscriptionRepository subscriptionRepository,
                               TransactionalOperator tx, WebSocketService webSocketService) {
        this.kafkaBootstrapServers = kafkaBootstrapServers;
        this.jsonMapper = jsonMapper;
        this.subscriptionRepository = subscriptionRepository;
        this.tx = tx;
        this.webSocketService = webSocketService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        var kafkaReceiver = KafkaReceiver.create(ReceiverOptions.<String, Message>create(Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers,
                        ConsumerConfig.GROUP_ID_CONFIG, "notification-service-notifications",
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest",
                        ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, false,
                        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true
                )).withKeyDeserializer(new StringDeserializer())
                .withValueDeserializer(new EventDeserializer())
                .subscription(List.of(
                        "encube.FileDescriptorChangedEvent",
                        "encube.WebSocketConnectionChangedEvent",
                        "encube.WebSocketMessageReceivedEvent"
                )));

        kafkaReceiver.receive()
                .flatMap(record -> {
                    log.info(
                            "Received event in NotificationService: key={}",
                            kv("key", record.key())
                    );
                    return processMessage(record.value())
                            .then(Mono.fromRunnable(() -> log.info(
                                    "Processed event in NotificationService: key={}",
                                    kv("key", record.key())
                            )))
                            .then(Mono.fromRunnable(() -> record.receiverOffset().acknowledge()));
                })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }

    private Mono<Void> processMessage(Message message) {
        return switch (message) {
            case WebSocketMessageReceivedEvent e -> processWebSocketMessageReceived(e);
            case FileDescriptorChangedEvent e -> processFileDescriptorChanged(e);
            case WebSocketConnectionChangedEvent e -> processWebSocketConnectionChanged(e);
            default -> Mono.empty();
        };
    }

    private Mono<Void> processFileDescriptorChanged(FileDescriptorChangedEvent e) {
        return switch (e.getChangeType()) {
            case CHANGE_TYPE_UPDATED, CHANGE_TYPE_CREATED -> {
                if (e.getNewValue().getState() != FileState.FILE_STATE_UPLOADED) {
                    log.info(
                            "Ignoring FileDescriptorChangedEvent for non-uploaded file: fileId={}",
                            kv("fileId", e.getNewValue().getId())
                    );

                    yield Mono.empty();
                }

                var canvasIdAsString = e.getNewValue().getAttributesMap().get("canvasId");

                if (canvasIdAsString == null) {
                    log.info(
                            "Ignoring FileDescriptorChangedEvent for file without canvasId attribute: fileId={}",
                            kv("fileId", e.getNewValue().getId())
                    );

                    yield Mono.empty();
                }

                var canvasId = Long.parseLong(canvasIdAsString);

                yield subscriptionRepository.findByCanvasId(canvasId)
                        .flatMap(subscription -> {
                            var payload = jsonMapper.writeValueAsString(FileUploadedMessage.builder()
                                    .fileId(e.getNewValue().getId())
                                    .fileName(e.getNewValue().getName())
                                    .contentType(e.getNewValue().getContentType())
                                    .build());

                            return webSocketService.sendMessageToConnection(subscription.payload().webSocketConnectionId(), new WebSocketMessage.Text(payload))
                                    .onErrorResume(NoSuchElementException.class, _ -> {
                                        log.info(
                                                "WebSocket connection {} not found for subscription {}, deleting subscription",
                                                kv("webSocketConnectionId", subscription.payload().webSocketConnectionId()),
                                                kv("subscriptionId", subscription.id())
                                        );

                                        return subscriptionRepository.deleteByWebSocketConnectionId(subscription.id())
                                                .as(tx::transactional)
                                                .then();
                                    });
                        })
                        .then();
            }
            default -> Mono.empty();
        };
    }

    @Builder(toBuilder = true)
    record FileUploadedMessage(Long fileId, String fileName, String contentType) {
    }

    private Mono<Void> processWebSocketMessageReceived(WebSocketMessageReceivedEvent e) {
        var subscriptionMessage = jsonMapper.readValue(e.getTextMessage(), SubscriptionMessage.class);

        if (subscriptionMessage == null) {
            return Mono.empty();
        }

        return switch (subscriptionMessage) {
            case SubscriptionMessage.Subscribe(Long canvasId) ->
                    subscriptionRepository.persist(Subscription.Payload.builder()
                                    .canvasId(canvasId)
                                    .webSocketConnectionId(e.getConnection().getId())
                                    .build())
                            .as(tx::transactional)
                            .doOnNext(subscriptionId -> {
                                log.info(
                                        "Created subscription {}, {}, {}",
                                        kv("subscriptionId", subscriptionId),
                                        kv("canvasId", canvasId),
                                        kv("webSocketConnectionId", e.getConnection().getId())
                                );
                            })
                            .then();
            case SubscriptionMessage.Unsubscribe(Long canvasId) ->
                    subscriptionRepository.deleteByCanvasIdAndWebSocketConnectionId(canvasId, e.getConnection().getId())
                            .then(Mono.fromRunnable(() -> {
                                log.info(
                                        "Deleted subscription: {}, {}",
                                        kv("canvasId", canvasId),
                                        kv("webSocketConnectionId", e.getConnection().getId())
                                );
                            }))
                            .as(tx::transactional)
                            .then();
        };
    }

    private Mono<Void> processWebSocketConnectionChanged(WebSocketConnectionChangedEvent e) {
        return switch (e.getChangeType()) {
            case CHANGE_TYPE_DELETED -> subscriptionRepository.deleteByWebSocketConnectionId(e.getOldValue().getId())
                    .as(tx::transactional);
            default -> Mono.empty();
        };
    }
}
