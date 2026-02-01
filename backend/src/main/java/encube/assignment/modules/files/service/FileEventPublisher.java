package encube.assignment.modules.files.service;

import com.google.protobuf.Timestamp;
import encube.assignment.events.*;
import encube.assignment.events.Header;
import encube.assignment.events.Subject;
import encube.assignment.modules.files.domain.FileDescriptor;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FileEventPublisher {

    private final EventPublisher eventPublisher;

    @Transactional(propagation = Propagation.MANDATORY)
    public Mono<Void> publishFileCreated(FileDescriptor fileDescriptor) {
        Validate.notNull(fileDescriptor, "fileDescriptor must not be null");

        return publishFilesCreated(List.of(fileDescriptor));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Mono<Void> publishFilesCreated(Collection<FileDescriptor> fileDescriptors) {
        Validate.notNull(fileDescriptors, "fileDescriptors must not be null");

        if (fileDescriptors.isEmpty()) {
            return Mono.empty();
        }

        var events = fileDescriptors.stream()
                .map(fd -> FileDescriptorChangedEvent.newBuilder()
                        .setHeader(headerOf(fd))
                        .setChangeType(ChangeType.CHANGE_TYPE_CREATED)
                        .setNewValue(toProtoFileDescriptor(fd))
                        .build())
                .toList();

        return eventPublisher.publish(events);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Mono<Void> publishFileUpdated(FileDescriptor fileDescriptor) {
        Validate.notNull(fileDescriptor, "fileDescriptor must not be null");

        return publishFilesUpdated(List.of(fileDescriptor));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Mono<Void> publishFilesUpdated(Collection<FileDescriptor> fileDescriptors) {
        Validate.notNull(fileDescriptors, "fileDescriptors must not be null");

        if (fileDescriptors.isEmpty()) {
            return Mono.empty();
        }

        var events = fileDescriptors.stream()
                .map(fd -> FileDescriptorChangedEvent.newBuilder()
                        .setHeader(headerOf(fd))
                        .setChangeType(ChangeType.CHANGE_TYPE_UPDATED)
                        .setNewValue(toProtoFileDescriptor(fd))
                        .build())
                .toList();

        return eventPublisher.publish(events);
    }

    private static encube.assignment.domain.FileDescriptor toProtoFileDescriptor(FileDescriptor fd) {
        return encube.assignment.domain.FileDescriptor.newBuilder()
                .setId(fd.id())
                .setState(switch (fd.state()) {
                    case DELETED -> encube.assignment.domain.FileState.FILE_STATE_DELETED;
                    case FAILED -> encube.assignment.domain.FileState.FILE_STATE_FAILED;
                    case PENDING -> encube.assignment.domain.FileState.FILE_STATE_PENDING;
                    case UPLOADED -> encube.assignment.domain.FileState.FILE_STATE_UPLOADED;
                })
                .setName(fd.payload().fileName())
                .setContentType(fd.payload().contentType())
                .build();
    }

    private Header headerOf(FileDescriptor fd) {
        return Header.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setSubject(Subject.newBuilder()
                        .setType("file-descriptor")
                        .setId(String.valueOf(fd.id()))
                        .build())
                .setTimestamp(now())
                .build();
    }

    private Timestamp now() {
        var now = Instant.now();

        return Timestamp.newBuilder()
                .setSeconds(now.getEpochSecond())
                .setNanos(now.getNano())
                .build();
    }
}
