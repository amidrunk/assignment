package encube.assignment.modules.notifications.domain;

import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Accessors(fluent = true)
@ToString
@EqualsAndHashCode
@Builder(toBuilder = true)
public class Subscription {

    private Long id;

    private Payload payload;

    @Builder(toBuilder = true)
    public record Payload(Long canvasId, Long webSocketConnectionId) {

    }
}
