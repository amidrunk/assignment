package encube.assignment.modules.files.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Component
@Slf4j
@ConditionalOnProperty(value = "file.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorage implements FileStorage, ApplicationRunner {

    public static final int DEFAULT_BUFFER_SIZE = 4096;

    private final Path localStoragePath;

    private final DataBufferFactory dataBufferFactory;

    public LocalFileStorage(@Value("${file.storage.local.path}") String localStoragePath, DataBufferFactory dataBufferFactory) {
        this.localStoragePath = Path.of(Validate.notNull(localStoragePath, "localStoragePath must not be null"));
        this.dataBufferFactory = Validate.notNull(dataBufferFactory, "dataBufferFactory must not be null");
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        java.nio.file.Files.createDirectories(localStoragePath);
    }

    @Override
    public Mono<Void> storeFile(Long fileId, Flux<DataBuffer> fileData) {
        Validate.notNull(fileId, "fileId must not be null");
        Validate.notNull(fileData, "fileData must not be null");

        var filePath = localStoragePath.resolve(String.valueOf(fileId));

        return DataBufferUtils.write(fileData, filePath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
                .doFinally(signalType -> {
                    log.info(
                            "Completed storing file {} {} {}",
                            kv("fileId", fileId),
                            kv("filePath", filePath.toString()),
                            kv("signalType", signalType)
                    );
                });
    }

    @Override
    public Flux<DataBuffer> retrieveFile(Long fileId) {
        Validate.notNull(fileId, "fileId must not be null");

        var filePath = localStoragePath.resolve(String.valueOf(fileId));

        return DataBufferUtils.read(filePath, dataBufferFactory, DEFAULT_BUFFER_SIZE);
    }
}
