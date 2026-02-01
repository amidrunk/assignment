package encube.assignment;

import com.google.protobuf.Message;
import encube.assignment.events.WebSocketConnectionChangedEvent;
import encube.assignment.modules.websocket.domain.WebSocketConnection;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@RequiredArgsConstructor
public class DomainEventReader {

    private final DatabaseClient db;

    public Flux<Message> all() {
        return db.sql("""
                select *
                from event_outbox
                """)
                .map(row -> {
                    try {
                        var typeName = row.get("protobuf_type_name", String.class);
                        var eventClassName = Class.forName(typeName);
                        var builder = eventClassName.getMethod("newBuilder").invoke(null);
                        var mergeFromMethod = builder.getClass().getMethod("mergeFrom", byte[].class);
                        var data = row.get("payload", byte[].class);

                        mergeFromMethod.invoke(builder, data);
                        var buildMethod = builder.getClass().getMethod("build");

                        return (Message) buildMethod.invoke(builder);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .all();
    }
}
