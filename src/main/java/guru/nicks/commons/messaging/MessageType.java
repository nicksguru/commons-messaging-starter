package guru.nicks.commons.messaging;

import guru.nicks.commons.messaging.resolver.MessageTypeResolver;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @see TypeAwareMessage#getType()
 */
public interface MessageType {

    /**
     * Needed for {@link MessageTypeResolver} to read/write the message type. Has better semantics than
     * {@link Object#toString()}.
     */
    @JsonIgnore
    String getMessageBrokerValue();

}
