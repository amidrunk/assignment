package encube.assignment.modules.websocket.repository;

import encube.assignment.modules.websocket.domain.WebSocketConnection;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Validate;
import org.intellij.lang.annotations.Language;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class WebSocketRepository {

    @Language("SQL")
    private final String SQL_PERSIST = """
            insert into web_socket_connection (session_id, user_name, host)
            values (:sessionId, :userName, :host)
            returning id
            """;

    @Language("SQL")
    private final String SQL_FIND_BY_ID = """
            select *
            from web_socket_connection
            where id = :id
            """;

    @Language("SQL")
    private final String SQL_DELETE_BY_ID = """
            delete from web_socket_connection
            where id = :id
            """;

    private final DatabaseClient db;

    @Transactional(propagation = Propagation.MANDATORY)
    public Mono<Long> persist(WebSocketConnection.Payload connection) {
        Validate.notNull(connection, "connection must not be null");

        return db.sql(SQL_PERSIST)
                .bind("sessionId", connection.sessionId())
                .bind("userName", connection.userName())
                .bind("host", connection.host())
                .map(row -> row.get("id", Long.class))
                .one();
    }

    public Mono<WebSocketConnection> findById(Long id) {
        Validate.notNull(id, "id must not be null");

        return db.sql(SQL_FIND_BY_ID)
                .bind("id", id)
                .map(WebSocketRepository::rowToWebSocketConnection)
                .one();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Mono<Void> deleteById(Long id) {
        Validate.notNull(id, "id must not be null");

        return db.sql(SQL_DELETE_BY_ID)
                .bind("id", id)
                .then();
    }

    private static WebSocketConnection rowToWebSocketConnection(io.r2dbc.spi.Readable row) {
        return WebSocketConnection.builder()
                .id(row.get("id", Long.class))
                .payload(new WebSocketConnection.Payload(
                        row.get("session_id", String.class),
                        row.get("user_name", String.class),
                        row.get("host", String.class)
                ))
                .build();
    }
}
