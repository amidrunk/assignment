package encube.assignment.modules.provisioning;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Component;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Component
@Profile("provision")
@Slf4j
public class DatabaseProvisioner implements ApplicationRunner {

    private final String databaseHost;

    private final Integer databasePort;

    private final String databaseName;

    private final String databaseUsername;

    private final String databasePassword;

    public DatabaseProvisioner(
            @Value("${database.host}") String databaseHost,
            @Value("${database.port}") Integer databasePort,
            @Value("${database.name}") String databaseName,
            @Value("${database.username}") String databaseUsername,
            @Value("${database.password}") String databasePassword
    ) {
        this.databaseHost = databaseHost;
        this.databasePort = databasePort;
        this.databaseName = databaseName;
        this.databaseUsername = databaseUsername;
        this.databasePassword = databasePassword;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        var dataSource = new SingleConnectionDataSource(
                String.format("jdbc:postgresql://%s:%d/%s", databaseHost, databasePort, databaseName),
                databaseUsername,
                databasePassword,
                true
        );

        log.info(
                "Starting database provisioning for {} {} {}",
                kv("host", databaseHost),
                kv("port", databasePort),
                kv("database", databaseName)
        );

        try (var connection = dataSource.getConnection()) {
            var liquibase = new liquibase.Liquibase(
                    "db/changelog/db.changelog-master.xml",
                    new liquibase.resource.ClassLoaderResourceAccessor(),
                    new liquibase.database.jvm.JdbcConnection(connection)
            );

            liquibase.update(new liquibase.Contexts());
        }
    }
}
