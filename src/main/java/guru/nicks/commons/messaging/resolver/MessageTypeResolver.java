package guru.nicks.commons.messaging.resolver;

import guru.nicks.commons.messaging.TypeAwareMessage;

import org.springframework.messaging.Message;

import java.util.Map;
import java.util.Optional;

/**
 * Defines how message types are retrieved out of raw messages and stored in them. All implementations are Spring beans,
 * and the primary one is used by default.
 */
public interface MessageTypeResolver {

    /**
     * Not {@code null} because it may not be allowed in map keys for mapping message types to consumers. All code MUST
     * refer to this constant for consistency.
     */
    String UNKNOWN_MESSAGE_TYPE = "";

    /**
     * Tries to find out message type.
     *
     * @param source message, must not be {@code null}
     * @return message type - if not detected, then {@link #UNKNOWN_MESSAGE_TYPE} ({@link Optional} would cause GC
     *         pressure under high load)
     */
    String readMessageType(Message<Map<String, Object>> source);

    /**
     * Stores message type in payload or headers.
     *
     * @param sourcePayload where to retrieve the message type from (with {@link TypeAwareMessage#getType()})
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
