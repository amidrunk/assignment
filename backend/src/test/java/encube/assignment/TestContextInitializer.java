package encube.assignment;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.MapPropertySource;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class TestContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    public static class InfrastructureConfig {

        @Bean(initMethod = "start", destroyMethod = "stop")
        public PostgreSQLContainer<?> psql() {
            return new PostgreSQLContainer<>(PostgreSQLContainer.IMAGE);
        }

        @Bean(destroyMethod = "destroy")
        public EmbeddedKafkaBroker kafka() {
            return new EmbeddedKafkaKraftBroker(1, 1);
        }
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        var parent = new AnnotationConfigApplicationContext(InfrastructureConfig.class);
        var psql = parent.getBean(PostgreSQLContainer.class);
        var kafka = parent.getBean(EmbeddedKafkaBroker.class);

        applicationContext.setParent(parent);

        final Path tempPath;

        try {
            tempPath = Files.createTempDirectory("file-storage-test-");
        } catch (IOException e) {
            throw new BeanCreationException("Failed to create temp directory for file storage", e);
        }

        tempPath.toFile().deleteOnExit();

        final int randomPort;

        try (var ss = new ServerSocket(0)) {
            randomPort = ss.getLocalPort();
        } catch (IOException e) {
            throw new BeanCreationException("Failed to find random port for test server", e);
        }

        applicationContext.getEnvironment().getPropertySources().addLast(new MapPropertySource("test-config", Map.of(
                "database.host", psql.getHost(),
                "database.port", psql.getFirstMappedPort().toString(),
                "database.name", psql.getDatabaseName(),
                "database.username", psql.getUsername(),
                "database.password", psql.getPassword(),
                "database.ssl", false,
                "file.storage.local.path", tempPath.toString(),
                "server.port", String.valueOf(randomPort),
                "kafka.bootstrap-servers", kafka.getBrokersAsString()
        )));
    }
}
