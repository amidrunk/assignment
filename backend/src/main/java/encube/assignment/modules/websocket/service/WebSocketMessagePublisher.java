package encube.assignment.modules.websocket.service;

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import encube.assignment.events.EventSerializer;
import encube.assignment.events.Header;
import encube.assignment.events.Subject;
import encube.assignment.events.WebSocketMessageReceivedEvent;
import encube.assignment.modules.websocket.domain.WebSocketConnection;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Component
@Slf4j
public class WebSocketMessagePublisher {

    private final KafkaSender<String, Message> kafkaSender;

    public WebSocketMessagePublisher(@Value("${kafka.bootstrap-servers}") String bootstrapServer) {
        this.kafkaSender = KafkaSender.create(SenderOptions.<String, Message>create(Map.of(
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer
                )).withKeySerializer(new StringSerializer())
                .withValueSerializer(new EventSerializer()));
    }

    public Mono<Void> publish(WebSocketConnection connection, WebSocketMessage message) {
        var now = Instant.now();
        var builder = WebSocketMessageReceivedEvent.newBuilder()
                .setHeader(Header.newBuilder()
                        .setSubject(Subject.newBuilder()
                                .setType("web-socket-message")
                                .setId(String.valueOf(connection.id()))
                                .build())
                        .setTimestamp(Timestamp.newBuilder()
                                .setSeconds(now.getEpochSecond())
                                .setNanos(now.getNano())
                                .build())
                        .setEventId(UUID.randomUUID().toString())
                        .build())
                .setConnection(encube.assignment.events.WebSocketConnection.newBuilder()
                        .setId(connection.id())
                        .setUserName(connection.payload().userName())
                        .build());

        switch (message) {
            case WebSocketMessage.Text(String payload) -> {
                builder.setTextMessage(payload);
            }
        }

        return kafkaSender.send(Flux.just(SenderRecord.create(
                new ProducerRecord<>(
                        "encube.WebSocketMessageReceivedEvent",
                        null,
                        String.valueOf(connection.id()),
                        builder.build()
                ), null
        ))).single().flatMap(result -> {
            if (result.exception() != null) {
                log.error(
                        "Failed to publish WebSocketMessageReceivedEvent for connection {}",
                        kv("connectionId", connection.id()),
                        result.exception()
                );

                return Mono.error(result.exception());
            } else {
                log.info(
                        "Published WebSocketMessageReceivedEvent for connection {}",
                        kv("connectionId", connection.id())
                );

                return Mono.empty();
            }
        }).then();
    }
}
