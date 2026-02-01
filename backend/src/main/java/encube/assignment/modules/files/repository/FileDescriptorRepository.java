package encube.assignment.modules.files.repository;

import encube.assignment.modules.files.domain.FileDescriptor;
import encube.assignment.modules.files.domain.exception.VersioningException;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Result;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Validate;
import org.intellij.lang.annotations.Language;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.Select;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.jooq.impl.DSL.*;

@Repository
@RequiredArgsConstructor
public class FileDescriptorRepository {

    private static final Query SQL_PERSIST = insertInto(table("file_descriptor"))
            .columns(
                    field("file_name"),
                    field("content_type"),
                    field("state"),
                    field("version")
            )
            .values(
                    field("$1"),
                    field("$2"),
                    field("$3::file_state"),
                    inline(1)
            )
            .returning(
                    field("id"),
                    field("version")
            );

    private static final Query SQL_UPDATE_STATE = update(table("file_descriptor"))
            .set(Map.of(
                    field("state"), field("$1::file_state"),
                    field("version"), field("version").plus(inline(1))
            ))
            .where(field("id").eq(field("$2"))
                    .and(field("version").eq(field("$3"))))
            .returning(field("version"));

    private static final Select<?> SQL_FIND_ALL = select(
            asterisk(),
            select(jsonArrayAgg(field("fa.*")).as("attributes"))
                    .from(table("file_attribute").as("fa"))
                    .where(field("fa.file_id").eq(field("fd.id")))
                    .asField())
            .from(table("file_descriptor").as("fd"));

    private static final Query SQL_FIND_ALL_BY_ATTRIBUTE = select(
            asterisk(),
            select(jsonArrayAgg(field("fa.*")).as("attributes"))
                    .from(table("file_attribute").as("fa"))
                    .where(field("fa.file_id").eq(field("fd.id")))
                    .asField())
            .from(table("file_descriptor").as("fd"))
            .where(exists(
                    selectOne()
                            .from(table("file_attribute").as("fa_filter"))
                            .where(field("fa_filter.file_id").eq(field("fd.id"))
                                    .and(field("fa_filter.name").eq(field("$1")))
                                    .and(field("fa_filter.value").eq(field("$2")))
                            )
            ));

    private static final Query SQL_FIND_BY_ID = select(asterisk())
            .from(SQL_FIND_ALL)
            .where(field("id").eq(field("$1")));

    private static final Query SQL_PERSIST_FILE_ATTRIBUTE = insertInto(table("file_attribute"))
            .columns(
                    field("file_id"),
                    field("name"),
                    field("value")
            )
            .values(
                    field("$1"),
                    field("$2"),
                    field("$3")
            );

    private final DatabaseClient db;

    private final DSLContext jooq;

    private final JsonMapper jsonMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public Flux<Tuple2<Long, Integer>> persist(FileDescriptor.State state, Collection<FileDescriptor.Payload> fileDescriptors) {
        Validate.notNull(state, "state must not be null");
        Validate.notNull(fileDescriptors, "fileDescriptors must not be null");

        if (fileDescriptors.isEmpty()) {
            return Flux.empty();
        }

        var persistFileSql = jooq.render(SQL_PERSIST);

        return db.inConnectionMany(connection -> {
            var statement = connection.createStatement(persistFileSql);

            for (var iterator = fileDescriptors.iterator(); iterator.hasNext(); ) {
                var fileDescriptor = iterator.next();

                statement.bind(0, fileDescriptor.fileName())
                        .bind(1, fileDescriptor.contentType())
                        .bind(2, state.name());

                if (iterator.hasNext()) {
                    statement.add();
                }
            }

            return Flux.zip(
                            Flux.from(statement.execute())
                                    .flatMap(result -> result.map((row) -> Tuples.of(
                                            row.get("id", Long.class),
                                            row.get("version", Integer.class)
                                    ))),
                            Flux.fromIterable(fileDescriptors)
                    )
                    .map(x -> Tuples.of(x.getT1().getT1(), x.getT1().getT2(), x.getT2()))
                    .flatMap(t -> {
                        var fileId = t.getT1();
                        var version = t.getT2();
                        var payload = t.getT3();

                        return persistFileAttributes(connection, fileId, payload.attributes())
                                .thenReturn(Tuples.of(fileId, version));
                    });
        });
    }

    private Mono<Void> persistFileAttributes(Connection connection, Long fileId, Map<String, String> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return Mono.empty();
        }

        var sql = jooq.render(SQL_PERSIST_FILE_ATTRIBUTE);
        var statement = connection.createStatement(sql);

        for (var iterator = attributes.entrySet().iterator(); iterator.hasNext(); ) {
            var entry = iterator.next();

            statement.bind(0, fileId)
                    .bind(1, entry.getKey())
                    .bind(2, entry.getValue());

            if (iterator.hasNext()) {
                statement.add();
            }
        }

        return Flux.from(statement.execute())
                .flatMap(Result::getRowsUpdated)
                .then();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Mono<Void> updateState(FileDescriptor.State newState, Collection<Tuple2<Long, Integer>> idAndVersions) {
        Validate.notNull(newState, "newState must not be null");
        Validate.notNull(idAndVersions, "idAndVersions must not be null");

        if (idAndVersions.isEmpty()) {
            return Mono.empty();
        }

        var sql = jooq.render(SQL_UPDATE_STATE);

        return db.inConnection(connection -> {
            var statement = connection.createStatement(sql);

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

        var sql = jooq.render(SQL_FIND_BY_ID);

        return db.inConnection(connection -> {
            var statement = connection.createStatement(sql)
                    .bind(0, id);

            return Mono.from(statement.execute())
                    .flatMap(result -> Mono.from(result.map((row) -> rowToFileDescriptor(row)
                    )));
        });
    }

    public Flux<FileDescriptor> findAll() {
        var sql = jooq.render(SQL_FIND_ALL);
        return db.inConnectionMany(connection -> Flux.from(connection.createStatement(sql).execute())
                .flatMap(result -> result.map(this::rowToFileDescriptor)));
    }

    public Flux<FileDescriptor> findAllByAttribute(String name, String value) {
        Validate.notNull(name, "name must not be null");
        Validate.notNull(value, "value must not be null");

        var sql = jooq.render(SQL_FIND_ALL_BY_ATTRIBUTE);

        return db.inConnectionMany(connection -> Flux.from(connection.createStatement(sql)
                        .bind(0, name)
                        .bind(1, value)
                        .execute())
                .flatMap(result -> result.map(this::rowToFileDescriptor)));
    }

    private FileDescriptor rowToFileDescriptor(io.r2dbc.spi.Readable row) {
        return FileDescriptor.builder()
                .id(row.get("id", Long.class))
                .state(FileDescriptor.State.valueOf(row.get("state", String.class)))
                .version(row.get("version", Integer.class))
                .payload(FileDescriptor.Payload.builder()
                        .fileName(row.get("file_name", String.class))
                        .contentType(row.get("content_type", String.class))
                        .attributes(Optional.ofNullable(row.get("attributes", String.class))
                                .map(jsonMapper::readTree)
                                .map(node -> StreamSupport.stream(node.spliterator(), false)
                                        .map(n -> Map.entry(n.get("name").asString(), n.get("value").asString()))
                                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                                .orElseGet(Map::of))
                        .build())
                .build();
    }
}
