package encube.assignment.modules.notifications.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SubscriptionMessage.Subscribe.class, name = "subscribe"),
        @JsonSubTypes.Type(value = SubscriptionMessage.Unsubscribe.class, name = "unsubscribe")
})
public sealed interface SubscriptionMessage permits SubscriptionMessage.Subscribe, SubscriptionMessage.Unsubscribe {

    record Subscribe(Long canvasId) implements SubscriptionMessage {}

    record Unsubscribe(Long canvasId) implements SubscriptionMessage {}
}
