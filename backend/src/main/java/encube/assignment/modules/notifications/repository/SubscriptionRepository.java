package encube.assignment.modules.notifications.repository;

import encube.assignment.modules.notifications.domain.Subscription;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Validate;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.jooq.impl.DSL.*;

@Repository
@RequiredArgsConstructor
public class SubscriptionRepository {

    private final Query SQL_PERSIST = insertInto(table("canvas_subscription"))
            .columns(field("canvas_id"), field("websocket_connection_id"))
            .values(field("$1"), field("$2"))
            .returning(field("id"));

    private final Query SQL_DELETE_BY_CANVAS_AND_CONNECTION = deleteFrom(table("canvas_subscription"))
            .where(field("canvas_id").eq(field("$1")))
            .and(field("websocket_connection_id").eq(field("$2")));

    private final Query SQL_DELETE_BY_WEBSOCKET_CONNECTION_ID = deleteFrom(table("canvas_subscription"))
            .where(field("websocket_connection_id").eq(field("$1")));

    private final Query SQL_FIND_BY_CANVAS_ID = select(asterisk())
            .from(table("canvas_subscription"))
            .where(field("canvas_id").eq(field("$1")));

    private final DatabaseClient db;

    private final DSLContext jooq;

    @Transactional(propagation = Propagation.MANDATORY)
    public Mono<Long> persist(Subscription.Payload payload) {
        Validate.notNull(payload, "payload must not be null");

        var sql = jooq.render(SQL_PERSIST);

        return db.sql(sql)
                .bind("$1", payload.canvasId())
                .bind("$2", payload.webSocketConnectionId())
                .map(row -> row.get("id", Long.class))
                .one();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Mono<Void> deleteByCanvasIdAndWebSocketConnectionId(Long canvasId, Long webSocketConnectionId) {
        Validate.notNull(canvasId, "canvasId must not be null");
        Validate.notNull(webSocketConnectionId, "webSocketConnectionId must not be null");

        var sql = jooq.render(SQL_DELETE_BY_CANVAS_AND_CONNECTION);

        return db.sql(sql)
                .bind("$1", canvasId)
                .bind("$2", webSocketConnectionId)
                .then();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Mono<Void> deleteByWebSocketConnectionId(Long webSocketConnectionId) {
        Validate.notNull(webSocketConnectionId, "webSocketConnectionId must not be null");

        var sql = jooq.render(SQL_DELETE_BY_WEBSOCKET_CONNECTION_ID);

        return db.sql(sql)
                .bind("$1", webSocketConnectionId)
                .then();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Flux<Subscription> findByCanvasId(Long canvasId) {
        Validate.notNull(canvasId, "canvasId must not be null");

        var sql = jooq.render(SQL_FIND_BY_CANVAS_ID);

        return db.sql(sql)
                .bind("$1", canvasId)
                .map(SubscriptionRepository::rowToSubscription)
                .all();
    }

    private static Subscription rowToSubscription(io.r2dbc.spi.Readable row) {
        return Subscription.builder()
                .id(row.get("id", Long.class))
                .payload(new Subscription.Payload(
                        row.get("canvas_id", Long.class),
                        row.get("websocket_connection_id", Long.class)
                ))
                .build();
    }

}
