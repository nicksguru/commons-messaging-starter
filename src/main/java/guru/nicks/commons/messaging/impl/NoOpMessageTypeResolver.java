package guru.nicks.commons.messaging.impl;

import guru.nicks.commons.messaging.TypeAwareMessage;
import guru.nicks.commons.messaging.resolver.MessageTypeResolver;
import guru.nicks.commons.messaging.service.MessagePublisherService;

import org.springframework.messaging.Message;

import java.util.Map;
import java.util.Optional;

/**
 * To be used in cases where a certain message topic has no concept of message types. Pass {@link #INSTANCE} to
 * {@link MessagePublisherService#publish(String, Object, Object, MessageTypeResolver)}. Or, make a message-related
 * service extend this interface and pass {@code this} to the above method.
 */
public interface NoOpMessageTypeResolver extends MessageTypeResolver {

    MessageTypeResolver INSTANCE = new NoOpMessageTypeResolver() {
    };

    @Override
    default Optional<String> readMessageType(Message<Map<String, Object>> source) {
        return Optional.empty();
    }

    @Override
    default void writeMessageType(TypeAwareMessage<?> sourcePayload, Map<String, Object> targetPayload,
            Map<String, Object> targetHeaders) {
        // do nothing
    }

    @Override
    default void writeMessageType(Map<String, Object> sourcePayload, Map<String, Object> targetPayload,
            Map<String, Object> targetHeaders) {
        // do nothing
    }

}
