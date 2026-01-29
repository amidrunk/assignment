package encube.assignment.modules.files.api.protocol;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import encube.assignment.modules.files.domain.FileDescriptor;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Accessors(fluent = true)
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class CreateFileRequest {

    @JsonUnwrapped
    private FileDescriptor.Payload fileDescriptor;
}
