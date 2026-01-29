package encube.assignment.modules.files.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Accessors(fluent = true)
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class FileDescriptor {

    public enum State {
        PENDING,
        UPLOADED,
        FAILED,
        DELETED
    }

    @JsonProperty("id")
    private Long id;

    @JsonProperty("state")
    private State state;

    @JsonProperty("version")
    private Integer version;

    @JsonUnwrapped
    private Payload payload;

    @Builder(toBuilder = true)
    public record Payload(String fileName, String contentType) {

    }
}
