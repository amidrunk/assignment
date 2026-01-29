package encube.assignment.modules.files.service;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FileStorage {

    Mono<Void> storeFile(Long fileId, Flux<DataBuffer> fileData);

    Flux<DataBuffer> retrieveFile(Long fileId);
}
