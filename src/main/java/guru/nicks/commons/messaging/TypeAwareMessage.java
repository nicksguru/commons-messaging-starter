package guru.nicks.commons.messaging;

import guru.nicks.commons.messaging.resolver.MessageTypeResolver;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

/**
 * Each {@link #getType()} is supposed to have a dedicated message class.
 */
public interface TypeAwareMessage<T extends MessageType> {

    /**
     * Rendered by {@link MessageTypeResolver#writeMessageType(TypeAwareMessage, Map, Map)}, not by JSON serialization.
     */
    @JsonIgnore
    T getType();

}
