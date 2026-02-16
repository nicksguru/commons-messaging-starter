package guru.nicks.commons.messaging.service;

import guru.nicks.commons.messaging.resolver.MessageTypeResolver;

import jakarta.annotation.Nullable;

/**
 * Message publisher.
 */
public interface MessagePublisherService {

    /**
     * Publishes to the given topic.
     *
     * @param topic      topic
     * @param payload    payload
     * @param messageKey message key ({@link Object#toString()} will be called if it's not {@code null}) affecting how
     *                   Kafka picks a partition for the message
     * @throws IllegalArgumentException payload class is not one of those that {@link MessageTypeResolver} accepts in
     *                                  its {@code writeMessageType} methods
     */
    void publish(String topic, Object payload, @Nullable Object messageKey, MessageTypeResolver messageTypeResolver);

    /**
     * Publishes payload to the given topic.
     *
     * @param topic      topic
     * @param payload    payload
     * @param messageKey message key affecting how Kafka picks a partition for the message
     * @throws IllegalArgumentException payload class is not one of those that {@link MessageTypeResolver} accepts in
     *                                  its {@code writeMessageType} methods
     */
    void publish(String topic, Object payload, @Nullable byte[] messageKey, MessageTypeResolver messageTypeResolver);

}
