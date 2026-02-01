package encube.assignment.modules.notifications.service;

import com.google.protobuf.Message;
import encube.assignment.events.EventDeserializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

import javax.sound.midi.Receiver;
import java.util.List;
import java.util.Map;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
@Slf4j
public class NotificationService implements ApplicationRunner {

    private final String kafkaBootstrapServers;

    public NotificationService(@Value("${kafka.bootstrap-servers}") String kafkaBootstrapServers) {
        this.kafkaBootstrapServers = kafkaBootstrapServers;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        var kafkaReceiver = KafkaReceiver.create(ReceiverOptions.<String, Message>create(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, "notification-service-notifications",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest",
                ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, false,
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true
        )).withKeyDeserializer(new StringDeserializer())
                .withValueDeserializer(new EventDeserializer())
                .subscription(List.of(
                        "encube.FileDescriptorChangedEvent"
                )));

        kafkaReceiver.receive()
                .flatMap(record -> {
                    log.info(
                            "Received event in NotificationService: key={}",
                            kv("key", record.key())
                    );
                    record.receiverOffset().acknowledge();
                    return Mono.empty();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }
}
