package encube.assignment.modules.canvas.service;

import com.google.protobuf.Timestamp;
import encube.assignment.events.*;
import encube.assignment.modules.canvas.domain.Canvas;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CanvasEventPublisher {

    private final EventPublisher eventPublisher;

    @Transactional(propagation = Propagation.MANDATORY)
    public Mono<Void> publishCanvasCreated(Canvas canvas) {
        Validate.notNull(canvas, "canvas must not be null");

        var now = Instant.now();

        return eventPublisher.publish(List.of(CanvasChangedEvent.newBuilder()
                .setChangeType(ChangeType.CHANGE_TYPE_CREATED)
                .setHeader(Header.newBuilder()
                        .setSubject(Subject.newBuilder()
                                .setType("canvas")
                                .setId(String.valueOf(canvas.id()))
                                .build())
                        .setTimestamp(Timestamp.newBuilder()
                                .setSeconds(now.getEpochSecond())
                                .setNanos(now.getNano())
                                .build())
                        .setEventId(UUID.randomUUID().toString())
                        .build())
                .setNewValue(toProtoCanvas(canvas))
                .build()));
    }

    private encube.assignment.domain.Canvas toProtoCanvas(Canvas canvas) {
        return encube.assignment.domain.Canvas.newBuilder()
                .setId(canvas.id())
                .setName(canvas.payload().name())
                .build();
    }

}
