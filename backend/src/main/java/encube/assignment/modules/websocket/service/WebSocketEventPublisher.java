package encube.assignment.modules.websocket.service;

import com.google.protobuf.Timestamp;
import encube.assignment.events.*;
import encube.assignment.modules.websocket.domain.WebSocketConnection;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WebSocketEventPublisher {

    private final EventPublisher eventPublisher;

    @Transactional(propagation = Propagation.MANDATORY)
    public Mono<Void> publishWebSocketConnectionCreated(WebSocketConnection connection) {
        return eventPublisher.publish(List.of(WebSocketConnectionChangedEvent.newBuilder()
                .setChangeType(ChangeType.CHANGE_TYPE_CREATED)
                .setHeader(headerOf(connection))
                        .setNewValue(toProtoWebSocketConnection(connection))
                .build()));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Mono<Void> publishWebSocketConnectionDeleted(WebSocketConnection connection) {
        return eventPublisher.publish(List.of(WebSocketConnectionChangedEvent.newBuilder()
                .setChangeType(ChangeType.CHANGE_TYPE_DELETED)
                .setHeader(headerOf(connection))
                        .setOldValue(toProtoWebSocketConnection(connection))
                .build()));
    }

    private static encube.assignment.events.WebSocketConnection toProtoWebSocketConnection(WebSocketConnection connection) {
        return encube.assignment.events.WebSocketConnection.newBuilder()
                .setId(connection.id())
                .setUserName(connection.payload().userName())
                .build();
    }

    private static @NonNull Header headerOf(WebSocketConnection connection) {
        var now = Instant.now();
        return Header.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setSubject(Subject.newBuilder()
                        .setType("web-socket-connection")
                        .setId(String.valueOf(connection.id()))
                        .build())
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(now.getEpochSecond())
                        .setNanos(now.getNano())
                        .build())
                .build();
    }
}
