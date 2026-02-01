package encube.assignment.modules.websocket.domain;

import lombok.*;
import lombok.experimental.Accessors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Builder(toBuilder = true)
@Getter
@Accessors(fluent = true)
@EqualsAndHashCode
@ToString
public class WebSocketConnection {

    private Long id;

    private Payload payload;

    @Builder(toBuilder = true)
    public record Payload(String sessionId, String userName, String host) {

    }
}
