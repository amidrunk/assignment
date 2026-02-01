package encube.assignment.modules.provisioning;

import com.google.protobuf.Message;
import encube.assignment.events.FileDescriptorChangedEvent;
import encube.assignment.events.WebSocketConnectionChangedEvent;
import encube.assignment.events.WebSocketMessageReceivedEvent;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Profile({"provision", "provision-kafka"})
public class KafkaProvisioner implements ApplicationRunner {

    private final List<Class<? extends Message>> MESSAGE_TYPES = List.of(
            FileDescriptorChangedEvent.class,
            WebSocketConnectionChangedEvent.class,
            WebSocketMessageReceivedEvent.class
    );

    private final String kafkaBootstrapServers;

    public KafkaProvisioner(@Value("${kafka.bootstrap-servers}") String kafkaBootstrapServers) {
        this.kafkaBootstrapServers = kafkaBootstrapServers;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        var adminClient = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers
        ));

        var newTopics = MESSAGE_TYPES.stream().map(messageType -> new NewTopic("encube." + messageType.getSimpleName(), 1, (short) 1)).toList();

        adminClient.createTopics(newTopics).all().get();
    }
}
