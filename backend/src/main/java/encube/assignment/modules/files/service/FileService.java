package encube.assignment.modules.files.service;

import encube.assignment.modules.files.domain.FileDescriptor;
import encube.assignment.modules.files.repository.FileDescriptorRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Validate;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FileService {

    private final TransactionalOperator tx;

    private final FileDescriptorRepository fileDescriptorRepository;

    private final FileStorage fileStorage;

    private final FileEventPublisher fileEventPublisher;

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public Mono<FileDescriptor> uploadFile(FileDescriptor.Payload fileDescriptorPayload, Flux<DataBuffer> fileData) {
        Validate.notNull(fileDescriptorPayload, "fileDescriptorPayload must not be null");
        Validate.notNull(fileData, "fileData must not be null");

        // materialize initial view (transactional)
        var initialFileDescriptorMono = tx.transactional(fileDescriptorRepository.persist(FileDescriptor.State.PENDING, List.of(fileDescriptorPayload)).single()
                .flatMap(idAndVersion -> fileDescriptorRepository.findById(idAndVersion.getT1()))
                .flatMap(newFileDescriptor -> fileEventPublisher.publishFileCreated(newFileDescriptor).thenReturn(newFileDescriptor)));

        // long-running operation (non-transactional - to avoid database connection pool exhaustion)
        var upload = initialFileDescriptorMono.flatMap(initialFileDescriptor -> fileStorage.storeFile(initialFileDescriptor.id(), fileData)
                .thenReturn(initialFileDescriptor));

        // finalize (transactional, versioned update [fails if concurrent modification detected])
        var finalize = upload.flatMap(initialFileDescriptor -> tx.transactional(fileDescriptorRepository.updateState(FileDescriptor.State.UPLOADED, List.of(Tuples.of(
                        initialFileDescriptor.id(),
                        initialFileDescriptor.version()
                )))
                .then(fileDescriptorRepository.findById(initialFileDescriptor.id()).flatMap(updatedFileDescriptor -> fileEventPublisher.publishFileUpdated(updatedFileDescriptor)
                        .thenReturn(updatedFileDescriptor)))));

        return finalize;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public Flux<FileDescriptor> listFiles() {
        return fileDescriptorRepository.findAll();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public Flux<FileDescriptor> listFilesByAttribute(String name, String value) {
        Validate.notNull(name, "name must not be null");
        Validate.notNull(value, "value must not be null");

        return fileDescriptorRepository.findAllByAttribute(name, value);
    }
}
