package encube.assignment.modules.files.api;

import encube.assignment.modules.files.api.protocol.CreateFileRequest;
import encube.assignment.modules.files.domain.FileDescriptor;
import encube.assignment.modules.files.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class FileApiController {

    private final FileService fileService;

    @PostMapping("/files")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<FileDescriptor> handleCreateFile(@AuthenticationPrincipal UserDetails user,
                                                      @RequestPart("descriptor") Mono<CreateFileRequest> request,
                                                      @RequestPart("file") FilePart file) {
        return request.flatMap(r -> fileService.uploadFile(r.fileDescriptor(), file.content()));
    }
}
