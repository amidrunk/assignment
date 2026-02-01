package encube.assignment.modules.websocket;

import com.google.protobuf.Message;
import encube.assignment.DomainEventReader;
import encube.assignment.IntegrationTest;
import encube.assignment.TestHelper;
import encube.assignment.client.WebSocketClientGrpc;
import encube.assignment.client.WebSocketMessageRequest;
import encube.assignment.domain.WebSocketMessagePayload;
import encube.assignment.events.ChangeType;
import encube.assignment.events.WebSocketConnectionChangedEvent;
import encube.assignment.events.WebSocketMessageReceivedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
public class WebSocketTest {

    @Autowired
    private DomainEventReader domainEventReader;

    @Autowired
    private WebSocketClient webSocketClient;

    @Value("${server.port}")
    private int port;

    @Value("${spring.webflux.base-path:}")
    private String basePath;

    @Autowired
    private TestHelper testHelper;

    @Autowired
    private KafkaReceiver<String, Message> eventReceiver;

    @Autowired
    private WebSocketClientGrpc.WebSocketClientBlockingV2Stub webSocketClientGrpc;

    @Test
    void domain_event_should_be_publish_on_web_socket_connection_and_disconnection() throws Exception {
        connect(WebSocketSession::close).block(Duration.ofSeconds(5));

        var connectEvent = domainEventReader.all()
                .filter(WebSocketConnectionChangedEvent.class::isInstance)
                .cast(WebSocketConnectionChangedEvent.class)
                .filter(e -> e.getChangeType() == ChangeType.CHANGE_TYPE_CREATED)
                .single()
                .retryWhen(Retry.fixedDelay(100, Duration.ofMillis(100)).filter(e -> e instanceof NoSuchElementException))
                .block(Duration.ofSeconds(5));

        assertThat(connectEvent.getChangeType()).isEqualTo(ChangeType.CHANGE_TYPE_CREATED);
        assertThat(connectEvent.getNewValue().getUserName()).isEqualTo("admin");

        var disconnectEvent = domainEventReader.all()
                .filter(WebSocketConnectionChangedEvent.class::isInstance)
                .cast(WebSocketConnectionChangedEvent.class)
                .filter(e -> e.getChangeType() == ChangeType.CHANGE_TYPE_DELETED)
                .single()
                .retryWhen(Retry.fixedDelay(100, Duration.ofMillis(100)).filter(e -> e instanceof NoSuchElementException))
                .block(Duration.ofSeconds(5));

        assertThat(disconnectEvent.getChangeType()).isEqualTo(ChangeType.CHANGE_TYPE_DELETED);
        assertThat(disconnectEvent.getOldValue().getUserName()).isEqualTo("admin");
    }

    @Test
    void message_received_from_user_should_be_published_as_domain_event() {
        String payload = "Hello, WebSocket!";

        var sink = Sinks.<WebSocketMessageReceivedEvent>one();

        connect(session -> session.send(
                        Mono.just(session.textMessage(payload))
                ).then(eventReceiver.receive().next().map(ReceiverRecord::value).cast(WebSocketMessageReceivedEvent.class))
                .doOnNext(sink::tryEmitValue)
                .then())
                .block();

        var receivedMessage = sink.asMono().block(Duration.ofSeconds(5));

        assertThat(receivedMessage.getTextMessage()).isEqualTo("Hello, WebSocket!");
    }

    @Test
    void message_can_be_sent_to_specific_websocket_connection() {
        var sink = Sinks.<WebSocketMessage>one();

        Flux.merge(
                        connect(session -> session.receive()
                                .next()
                                .doOnNext(sink::tryEmitValue)
                                .then()),
                        domainEventReader.all()
                                .filter(WebSocketConnectionChangedEvent.class::isInstance)
                                .cast(WebSocketConnectionChangedEvent.class)
                                .filter(e -> e.getChangeType() == ChangeType.CHANGE_TYPE_CREATED)
                                .single()
                                .retryWhen(Retry.fixedDelay(100, Duration.ofMillis(100)).filter(e -> e instanceof NoSuchElementException))
                                .flatMap(event -> {
                                    var connectionId = event.getNewValue().getId();

                                    return Mono.fromCallable(() -> {
                                        return webSocketClientGrpc.sendMessage(WebSocketMessageRequest.newBuilder()
                                                .setConnectionId(connectionId)
                                                .setMessage(WebSocketMessagePayload.newBuilder()
                                                        .setTextMessage("Hello World!")
                                                        .build())
                                                .build());
                                    }).flatMap(response -> {
                                        if (response.hasError()) {
                                            return Mono.error(new IllegalStateException("Failed to send WebSocket message: " + response.getError().getMessage()));
                                        } else {
                                            return Mono.empty();
                                        }
                                    });
                                })
                )
                .then()
                .block();
    }

    private Mono<Void> connect(Function<WebSocketSession, Mono<Void>> messageHandler) {
        var sessionCookie = testHelper.login();

        var headers = new HttpHeaders();

        headers.set(HttpHeaders.COOKIE, sessionCookie.getName() + "=" + sessionCookie.getValue());

        String wsBasePath = basePath != null ? basePath : "";

        return webSocketClient.execute(
                URI.create("ws://localhost:" + port + wsBasePath + "/ws"),
                headers,
                messageHandler::apply
        );
    }
}
