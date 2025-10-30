package guru.nicks.messaging;

import guru.nicks.messaging.resolver.MessageTypeResolver;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

/**
 * Each {@link #getType()} is supposed to have a dedicated message class.
 */
public interface TypeAwareMessage<T extends MessageType> {

    /**
     * This is simply {@code ''} ({@code null} may not be allowed in map keys/values). All code must refer to this
     * constant for consistency.
     */
    String UNKNOWN_MESSAGE_TYPE = "";

    /**
     * Rendered by {@link MessageTypeResolver#writeMessageType(TypeAwareMessage, Map, Map)}, not by JSON serialization.
     */
    @JsonIgnore
    T getType();

}
