package encube.assignment.modules.canvas.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Accessors(fluent = true)
@ToString
@EqualsAndHashCode
@Builder(toBuilder = true)
public class Canvas {

    @JsonProperty("id")
    private Long id;

    @JsonUnwrapped
    private Payload payload;

    @Builder(toBuilder = true)
    public record Payload(String name) {

    }
}
