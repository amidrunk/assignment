package encube.assignment.events;

import com.google.protobuf.Message;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.StandardCharsets;

public class EventSerializer implements Serializer<Message> {

    @Override
    public byte[] serialize(String topic, Message data) {
        return data.toByteArray();
    }

    @Override
    public byte[] serialize(String topic, Headers headers, Message data) {
        headers.add(new RecordHeader("protobuf_type_name", data.getClass().getName().getBytes(StandardCharsets.UTF_8)));
        return data.toByteArray();
    }
}
