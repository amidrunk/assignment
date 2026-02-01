package encube.assignment.modules.websocket.service;

import encube.assignment.modules.websocket.domain.WebSocketConnection;
import encube.assignment.modules.websocket.repository.WebSocketRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
@Slf4j
public class WebSocketService {

    private final WebSocketRepository webSocketRepository;

    private final TransactionalOperator tx;

    private final int localPort;

    private final WebSocketEventPublisher webSocketEventPublisher;
    private final WebSocketMessagePublisher webSocketMessagePublisher;

    public WebSocketService(WebSocketRepository webSocketRepository, TransactionalOperator tx, @Value("${server.port}") int localPort, WebSocketEventPublisher webSocketEventPublisher, WebSocketMessagePublisher webSocketMessagePublisher) {
        this.webSocketRepository = webSocketRepository;
        this.tx = tx;
        this.localPort = localPort;
        this.webSocketEventPublisher = webSocketEventPublisher;
        this.webSocketMessagePublisher = webSocketMessagePublisher;
    }

    public Mono<Void> connectWebSocket(WebSocketSession session) {
        var hostName = resolveHostNameOfThePodIAmRunningIn();
        return session.getHandshakeInfo().getPrincipal().flatMap(principal -> {
            return tx.transactional(webSocketRepository.persist(WebSocketConnection.Payload.builder()
                                    .host(hostName)
                                    .userName(principal.getName())
                                    .sessionId(session.getId())
                                    .build())
                            .flatMap(webSocketRepository::findById)
                            .flatMap(webSocketConnection -> {
                                return webSocketEventPublisher.publishWebSocketConnectionCreated(webSocketConnection)
                                        .thenReturn(webSocketConnection);
                            }))
                    .doOnError(e -> {
                        log.error(
                                "Error while connecting WebSocket for session {} and user {}",
                                kv("sessionId", session.getId()),
                                kv("userName", principal.getName()),
                                e
                        );
                    })
                    .flatMap(webSocketConnection -> {
                        var outbox = Sinks.many().unicast().<WebSocketMessage>onBackpressureBuffer();


                        return Flux.merge(
                                outbox.asFlux()
                                        .flatMap(outgoingMessage -> switch (outgoingMessage) {
                                            case WebSocketMessage.Text(String payload) -> session.send(Mono.just(session.textMessage(payload)));
                                        }),
                                session.receive()
                                        .flatMap(webSocketMessage -> {
                                            var message = switch (webSocketMessage.getType()) {
                                                case TEXT -> new WebSocketMessage.Text(webSocketMessage.getPayloadAsText());
                                                default -> null;
                                            };

                                            if (message == null) {
                                                return Mono.empty();
                                            }

                                            return webSocketMessagePublisher.publish(webSocketConnection, message);
                                        })
                        ).then(Mono.just(webSocketConnection));
                    })
                    .flatMap(webSocketConnection -> {
                        return tx.transactional(webSocketRepository.deleteById(webSocketConnection.id())
                                .then(webSocketEventPublisher.publishWebSocketConnectionDeleted(webSocketConnection)));
                    });
        });
    }

    public Mono<Void> sendMessageToUser(String userName, WebSocketMessage message) {
        return  Mono.empty();
    }

    public Mono<Void> sendMessageToConnection(Long id, WebSocketMessage message) {
        return Mono.empty();
    }

    private String resolveHostNameOfThePodIAmRunningIn() {
        var hostName = System.getenv("HOSTNAME");

        if (hostName != null) {
            return hostName + ":" + localPort;
        }

        try {
            var localHost = InetAddress.getLocalHost();

            return localHost.getHostName() + ":" + localPort;
        } catch (UnknownHostException e) {
            throw new RuntimeException("Unable to resolve hostname", e);
        }
    }
}
