package encube.assignment;

import com.google.protobuf.Message;
import encube.assignment.client.WebSocketClientGrpc;
import encube.assignment.events.EventDeserializer;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.netty.http.client.HttpClient;

import java.util.List;
import java.util.Map;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Configuration
@Slf4j
public class TestConfig {

    @Bean
    public WebTestClient webTestClient(
            @Value("${server.port}") int port,
            @Value("${spring.webflux.base-path:}") String basePath
    ) {
        String baseUrl = "http://localhost:" + port + (basePath == null ? "" : basePath);
        // Bind to the running server so cookies/sessions behave the same way as in production
        return WebTestClient.bindToServer()
                .baseUrl(baseUrl)
                .build();
    }

    @Bean
    public WebSocketClient webSocketClient() {
        return new ReactorNettyWebSocketClient(HttpClient.create());
    }

    @Bean
    public KafkaReceiver<String, Message> webSocketMessageReceiver(@Value("${kafka.bootstrap-servers}") String bootstrapServers) {
        return KafkaReceiver.create(ReceiverOptions.<String, Message>create(Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                        ConsumerConfig.GROUP_ID_CONFIG, "test-websocket-message-receiver",
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"
                )).withKeyDeserializer(new StringDeserializer())
                .withValueDeserializer(new EventDeserializer()).subscription(List.of("encube.WebSocketMessageReceivedEvent")));
    }

    @Bean
    public WebSocketClientGrpc.WebSocketClientBlockingV2Stub webSocketClientGrpcStub(
            @Value("${grpc.server.port}") int grpcPort
    ) {
        var channel = ManagedChannelBuilder.forTarget("localhost:" + grpcPort)
                .usePlaintext()
                .build();

        log.info(
                "Configured gRPC WebSocketClientGrpc stub on {} {}",
                kv("host", "localhost"),
                kv("port", grpcPort)
        );

        return WebSocketClientGrpc.newBlockingV2Stub(channel);
    }
}
