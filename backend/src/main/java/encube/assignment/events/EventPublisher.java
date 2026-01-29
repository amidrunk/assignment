package encube.assignment.events;

import com.google.protobuf.Message;
import io.r2dbc.spi.Result;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Validate;
import org.intellij.lang.annotations.Language;
import org.springframework.context.MessageSource;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class EventPublisher {

    @Language("sql")
    private static final String SQL_PERSIST = """
            insert into event_outbox (id, aggregatetype, aggregateid, type, protobuf_type_name, payload)
            values ($1::uuid, $2, $3, $4, $5, $6)
            """;

    private final DatabaseClient db;
    private final MessageSource messageSource;

    @Transactional(propagation = Propagation.MANDATORY)
    public Mono<Void> publish(Collection<? extends Message> messages) {
        Validate.notNull(messages, "messages must not be null");

        if (messages.isEmpty()) {
            return Mono.empty();
        }

        return db.inConnection(connection -> {
            var statement = connection.createStatement(SQL_PERSIST);

            for (var iterator = messages.iterator(); iterator.hasNext(); ) {
                var message = iterator.next();
                var header = (Header) message.getAllFields().entrySet().stream()
                        .filter(f -> Objects.equals(f.getKey().getName(), "header"))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElseThrow();
                var id = header.getEventId();
                var aggregateType = header.getSubject().getType();
                var aggregateId = header.getSubject().getId();
                var type = message.getDescriptorForType().getName();
                var protobufTypeName = message.getDescriptorForType().getFullName();
                var payload = message.toByteArray();

                statement.bind("$1", id)
                        .bind("$2", aggregateType)
                        .bind("$3", aggregateId)
                        .bind("$4", type)
                        .bind("$5", protobufTypeName)
                        .bind("$6", payload);

                if (iterator.hasNext()) {
                    statement.add();
                }
            }

            return Flux.from(statement.execute())
                    .flatMap(Result::getRowsUpdated)
                    .then();
        });
    }
}
