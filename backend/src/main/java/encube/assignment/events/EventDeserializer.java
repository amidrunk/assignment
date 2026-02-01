package encube.assignment.events;

import com.google.protobuf.Message;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

public class EventDeserializer implements Deserializer<Message> {

    @Override
    public Message deserialize(String topic, byte[] data) {
        throw new UnsupportedOperationException("Use deserialize with Headers");
    }

    @Override
    public Message deserialize(String topic, Headers headers, byte[] data) {
        var typeHeader = headers.lastHeader("protobuf_type_name");

        if (typeHeader == null) {
            throw new IllegalArgumentException("Missing protobuf_type_name header");
        }

        var typeName = new String(typeHeader.value(), StandardCharsets.UTF_8);

        final Class<?> clazz;

        try {
            clazz = Class.forName(typeName);
        } catch (ClassNotFoundException e) {
            throw new EventDeserializationException(
                    "Failed to find class for protobuf type name: " + typeName,
                    e
            );
        }

        if (!Message.class.isAssignableFrom(clazz)) {
            throw new EventDeserializationException(
                    "Class " + typeName + " is not a subclass of com.google.protobuf.Message"
            );
        }

        final Method newBuilder;

        try {
            newBuilder = clazz.getMethod("newBuilder");
        } catch (NoSuchMethodException e) {
            throw new EventDeserializationException(
                    "Failed to find newBuilder method for class: " + typeName,
                    e
            );
        }

        final Object builder;

        try {
            builder = newBuilder.invoke(null);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new EventDeserializationException(
                    "Failed to invoke newBuilder method for class: " + typeName,
                    e
            );
        }

        final Method mergeFrom;

        try {
            mergeFrom = builder.getClass().getMethod("mergeFrom", byte[].class);
        } catch (NoSuchMethodException e) {
            throw new EventDeserializationException(
                    "Failed to find mergeFrom method for builder of class: " + typeName,
                    e
            );
        }

        try {
            mergeFrom.invoke(builder, data);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new EventDeserializationException(
                    "Failed to invoke mergeFrom method for builder of class: " + typeName,
                    e
            );
        }

        final Method build;

        try {
            build = builder.getClass().getMethod("build");
        } catch (NoSuchMethodException e) {
            throw new EventDeserializationException(
                    "Failed to find build method for builder of class: " + typeName,
                    e
            );
        }

        try {
            return (Message) build.invoke(builder);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new EventDeserializationException(
                    "Failed to invoke build method for builder of class: " + typeName,
                    e
            );
        }
    }
}
