package encube.assignment.modules.files.api;

import encube.assignment.modules.files.api.protocol.CreateFileRequest;
import encube.assignment.modules.files.domain.FileDescriptor;
import encube.assignment.modules.files.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST API controller for managing file operations such as searching and uploading files.
 */
@RestController
@RequiredArgsConstructor
public class FileApiController {

    private final FileService fileService;

    @GetMapping("/files")
    @ResponseStatus(HttpStatus.OK)
    public Flux<FileDescriptor> handleSearchFiles(@AuthenticationPrincipal UserDetails user,
                                                  @RequestParam(name = "canvasId", required = false) String canvasId) {
        if (canvasId != null) {
            return fileService.listFilesByAttribute("canvasId", canvasId);
        }

        return fileService.listFiles();
    }

    @PostMapping("/files")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<FileDescriptor> handleCreateFile(@AuthenticationPrincipal UserDetails user,
                                                      @RequestPart("descriptor") Mono<CreateFileRequest> request,
                                                      @RequestPart("file") FilePart file) {
        return request.flatMap(r -> fileService.uploadFile(r.fileDescriptor(), file.content()));
    }

    @GetMapping("/files/{fileId}/data")
    public Mono<ResponseEntity<Flux<DataBuffer>>> handleGetFileData(@PathVariable String fileId) {
        return fileService.getFileData(Long.parseLong(fileId))
                .map(t -> {
                    var fileDescriptor = t.getT1();
                    var dataBuffer = t.getT2();

                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(
                                    fileDescriptor.payload().contentType()
                            ))
                            .body(dataBuffer);
                });
    }
}
