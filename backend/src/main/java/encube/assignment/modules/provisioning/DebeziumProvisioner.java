package encube.assignment.modules.provisioning;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

@Component
@Profile("provision")
@Slf4j
public class DebeziumProvisioner implements ApplicationRunner {

    private final WebClient webClient;

    private final String debeziumHost;

    private final int debeziumPort;

    private final String databaseHostName;

    private final int databasePort;

    private final String databaseUser;

    private final String databasePassword;

    private final String databaseName;

    private final String kafkaBootstrapServers;

    public DebeziumProvisioner(WebClient webClient,
                               @Value("${debezium.host}") String debeziumHost,
                               @Value("${debezium.port}") int debeziumPort,
                               @Value("${database.host}") String databaseHostName,
                               @Value("${database.port}") int databasePort,
                               @Value("${database.username}") String databaseUser,
                               @Value("${database.password}") String databasePassword,
                               @Value("${database.name}") String databaseName,
                               @Value("${kafka.bootstrap-servers}") String kafkaBootstrapServers) {
        this.webClient = webClient;
        this.debeziumHost = debeziumHost;
        this.debeziumPort = debeziumPort;
        this.databaseHostName = databaseHostName;
        this.databasePort = databasePort;
        this.databaseUser = databaseUser;
        this.databasePassword = databasePassword;
        this.databaseName = databaseName;
        this.kafkaBootstrapServers = kafkaBootstrapServers;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Mono.defer(() -> {
                    return webClient.post()
                            .uri("http://" + debeziumHost + ":" + debeziumPort + "/connectors")
                            .header("Content-Type", "application/json")
                            .bodyValue(getConnector())
                            .exchangeToMono(response -> {
                                if (response.statusCode().is2xxSuccessful()) {
                                    return response.bodyToMono(String.class)
                                            .doOnNext(body -> log.info("Debezium connector provisioned successfully: {}", body));
                                } else if (response.statusCode() == HttpStatus.CONFLICT) {
                                    log.info(
                                            "Debezium connector already exists, skipping provisioning"
                                    );

                                    return reactor.core.publisher.Mono.empty();
                                } else {
                                    return response.bodyToMono(String.class)
                                            .defaultIfEmpty("")
                                            .flatMap(body -> {
                                                log.error("Failed to provision Debezium connector: status={}, body={}",
                                                        response.statusCode().value(), body);
                                                return reactor.core.publisher.Mono.error(
                                                        new RuntimeException("Failed to provision Debezium connector: status " + response.statusCode().value()));
                                            });
                                }
                            }).then();
                })
                .retryWhen(Retry.fixedDelay(30, Duration.ofSeconds(1))
                        .filter(e -> e instanceof WebClientRequestException)
                        .doBeforeRetry(signal -> {
                            log.info(
                                    "Debezium server not available yet, retrying provisioning... (attempt {})",
                                    signal.totalRetries() + 1
                            );
                        }))
                .block();
    }

    private Connector getConnector() {
        return Connector.builder()
                .name("encube-connector")
                .config(Map.ofEntries(
                        Map.entry("connector.class", "io.debezium.connector.postgresql.PostgresConnector"),
                        Map.entry("database.hostname", databaseHostName),
                        Map.entry("database.port", String.valueOf(databasePort)),
                        Map.entry("database.user", databaseUser),
                        Map.entry("database.password", databasePassword),
                        Map.entry("database.dbname", databaseName),
                        Map.entry("database.server.name", "encube-database"),
                        Map.entry("table.include.list", "public.event_outbox"),
                        Map.entry("database.history.kafka.bootstrap.servers", kafkaBootstrapServers),
                        Map.entry("plugin.name", "pgoutput"),
                        Map.entry("transforms", "outbox"),
                        Map.entry("transforms.outbox.type", "io.debezium.transforms.outbox.EventRouter"),
                        Map.entry("transforms.outbox.table.fields.additional.placement", "type:header:protobuf_type_name,aggregateid:header"),
                        Map.entry("transforms.outbox.route.topic.replacement", "encube.${routedByValue}"),
                        Map.entry("value.converter", "io.debezium.converters.BinaryDataConverter"),
                        Map.entry("topic.prefix", "encube")
                ))
                .build();
    }

    @Builder(toBuilder = true)
    record Connector(String name, Map<String, String> config) {
    }
}
