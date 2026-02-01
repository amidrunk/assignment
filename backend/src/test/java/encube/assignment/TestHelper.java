package encube.assignment;

import com.google.protobuf.Message;
import encube.assignment.events.EventDeserializer;
import encube.assignment.events.WebSocketMessageReceivedEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Component
@RequiredArgsConstructor
public class TestHelper {

    private final WebTestClient webTestClient;

    @Value("${server.port}")
    private int port;

    @Value("${spring.webflux.base-path:}")
    private String basePath;

    public org.springframework.http.ResponseCookie login() {
        String baseUrl = "http://localhost:" + port + (basePath != null ? basePath : "");

        var client = WebTestClient.bindToServer()
                .baseUrl(baseUrl)
                .build();

        var result = client.post()
                .uri("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("username", "admin")
                        .with("password", "changeme"))
                .exchange()
                .returnResult(Void.class);

        var session = result.getResponseCookies().getFirst("SESSION");
        assertThat(session).as("session cookie from login").isNotNull();
        return session;
    }

    public WebTestClient authenticatedClient() {
        var sessionCookie = login();

        return webTestClient.mutate()
                .defaultCookie(sessionCookie.getName(), sessionCookie.getValue())
                .build();
    }
}
