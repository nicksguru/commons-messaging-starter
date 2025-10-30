package guru.nicks.messaging.resolver;

import guru.nicks.messaging.TypeAwareMessage;

import org.springframework.messaging.Message;

import java.util.Map;
import java.util.Optional;

/**
 * Defines how message types are retrieved out of raw messages and stored in them. All implementations are Spring beans,
 * and the primary one is used by default.
 */
public interface MessageTypeResolver {

    /**
     * Finds out message type.
     *
     * @param source message
     * @return message type - if detected, then a non-blank string
     */
    Optional<String> readMessageType(Message<Map<String, Object>> source);

    /**
     * Stores message type in payload or headers.
     *
     * @param sourcePayload where to retrieve the message type from (using {@link TypeAwareMessage#getType()})
     * @param targetPayload where to store the message type, in case it needs to be stored in payload
     * @param targetHeaders headers container, in case the message type needs to be stored in a header
     */
    void writeMessageType(TypeAwareMessage<?> sourcePayload, Map<String, Object> targetPayload,
            Map<String, Object> targetHeaders);

    /**
     * Stores message type in payload or headers.
     *
     * @param sourcePayload where to retrieve the message type from
     * @param targetPayload where to store the message type, in case it needs to be stored in payload
     * @param targetHeaders headers container, in case the message type needs to be stored in a header
     */
    void writeMessageType(Map<String, Object> sourcePayload, Map<String, Object> targetPayload,
            Map<String, Object> targetHeaders);

}
