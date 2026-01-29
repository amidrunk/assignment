package encube.assignment.modules.files.repository;

import encube.assignment.modules.files.domain.FileDescriptor;
import encube.assignment.modules.files.domain.exception.VersioningException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Validate;
import org.intellij.lang.annotations.Language;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Collection;

@Repository
@RequiredArgsConstructor
public class FileDescriptorRepository {

    @Language("sql")
    private final String SQL_PERSIST = """
            INSERT INTO file_descriptor (file_name, content_type, state, version)
            VALUES ($1, $2, $3::file_state, 1)
            RETURNING id, version
            """;

    @Language("sql")
    private final String SQL_UPDATE_STATE = """
            UPDATE file_descriptor
            SET state = $1::file_state, version = version + 1
            WHERE id = $2 and version = $3
            RETURNING version
            """;

    @Language("sql")
    private final String SQL_FIND_BY_ID = """
            SELECT file_descriptor.*
            FROM file_descriptor
            WHERE id = $1
            """;

    private final DatabaseClient db;

    @Transactional(propagation = Propagation.MANDATORY)
    public Flux<Tuple2<Long, Integer>> persist(FileDescriptor.State state, Collection<FileDescriptor.Payload> fileDescriptors) {
        Validate.notNull(state, "state must not be null");
        Validate.notNull(fileDescriptors, "fileDescriptors must not be null");

        if (fileDescriptors.isEmpty()) {
            return Flux.empty();
        }

        return db.inConnectionMany(connection -> {
            var statement = connection.createStatement(SQL_PERSIST);

            for (var iterator = fileDescriptors.iterator(); iterator.hasNext(); ) {
                var fileDescriptor = iterator.next();

                statement.bind(0, fileDescriptor.fileName())
                        .bind(1, fileDescriptor.contentType())
                        .bind(2, state.name());

                if (iterator.hasNext()) {
                    statement.add();
                }
            }

            return Flux.from(statement.execute())
                    .flatMap(result -> result.map((row) -> Tuples.of(
                            row.get("id", Long.class),
                            row.get("version", Integer.class)
                    )));
        });
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Mono<Void> updateState(FileDescriptor.State newState, Collection<Tuple2<Long, Integer>> idAndVersions) {
        Validate.notNull(newState, "newState must not be null");
        Validate.notNull(idAndVersions, "idAndVersions must not be null");

        if (idAndVersions.isEmpty()) {
            return Mono.empty();
        }

        return db.inConnection(connection -> {
            var statement = connection.createStatement(SQL_UPDATE_STATE);

            for (var iterator = idAndVersions.iterator(); iterator.hasNext(); ) {
                var idAndVersion = iterator.next();

                statement.bind(0, newState.name())
                        .bind(1, idAndVersion.getT1())
                        .bind(2, idAndVersion.getT2());

                if (iterator.hasNext()) {
                    statement.add();
                }
            }

            return Flux.from(statement.execute())
                    .flatMap(result -> Flux.from(result.getRowsUpdated())
                            .flatMap(rowsUpdated -> {
                                if (rowsUpdated == 0) {
                                    return Mono.error(new VersioningException("Failed to update state for some file descriptors due to version mismatch"));
                                } else {
                                    return Mono.empty();
                                }
                            }))
                    .then();
        });
    }

    public Mono<FileDescriptor> findById(Long id) {
        Validate.notNull(id, "id must not be null");

        return db.inConnection(connection -> {
            var statement = connection.createStatement(SQL_FIND_BY_ID)
                    .bind(0, id);

            return Mono.from(statement.execute())
                    .flatMap(result -> Mono.from(result.map((row) -> rowToFileDescriptor(row)
                    )));
        });
    }

    private static FileDescriptor rowToFileDescriptor(io.r2dbc.spi.Readable row) {
        return FileDescriptor.builder()
                .id(row.get("id", Long.class))
                .state(FileDescriptor.State.valueOf(row.get("state", String.class)))
                .version(row.get("version", Integer.class))
                .payload(new FileDescriptor.Payload(
                        row.get("file_name", String.class),
                        row.get("content_type", String.class)
                ))
                .build();
    }
}
