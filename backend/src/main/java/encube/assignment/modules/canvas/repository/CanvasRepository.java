package encube.assignment.modules.canvas.repository;

import encube.assignment.modules.canvas.domain.Canvas;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Validate;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.Select;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.jooq.impl.DSL.*;

@Repository
@RequiredArgsConstructor
public class CanvasRepository {

    private static final Query SQL_PERSIST = insertInto(table("canvas"))
            .columns(field("name"))
            .values(field(":name"))
            .returning(field("id"));

    private static final Select<?> SQL_ALL = select(asterisk())
            .from(table("canvas"));

    private static final Query SQL_BY_ID = select(asterisk())
            .from(SQL_ALL)
            .where(field("id").eq(field(":id")));

    private final DatabaseClient db;

    private final DSLContext jooq;

    @Transactional(propagation = Propagation.MANDATORY)
    public Mono<Long> persist(Canvas.Payload payload) {
        Validate.notNull(payload, "Canvas payload must not be null");

        var sql = jooq.render(SQL_PERSIST);

        return db.sql(sql)
                .bind("name", payload.name())
                .map(row -> row.get("id", Long.class))
                .one();
    }

    public Mono<Canvas> findById(Long id) {
        Validate.notNull(id, "Canvas id must not be null");

        var sql = jooq.render(SQL_BY_ID);

        return db.sql(sql)
                .bind("id", id)
                .map(row -> rowToCanvas(row))
                .one();
    }

    public Flux<Canvas> findAll() {
        var sql = jooq.render(SQL_ALL);

        return db.sql(sql)
                .map(row -> rowToCanvas(row))
                .all();
    }

    private static Canvas rowToCanvas(io.r2dbc.spi.Readable row) {
        return Canvas.builder()
                .id(row.get("id", Long.class))
                .payload(Canvas.Payload.builder()
                        .name(row.get("name", String.class))
                        .build())
                .build();
    }
}
