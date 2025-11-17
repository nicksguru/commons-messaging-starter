package guru.nicks.commons.messaging.service;

import guru.nicks.commons.messaging.resolver.MessageTypeResolver;

import jakarta.annotation.Nullable;

/**
 * Message publisher.
 */
public interface MessagePublisherService {

    /**
     * Publishes to the given topic.
     * <p>
     * NOTE: producer must be configured to borrow the partition key from {@code headers['partitionKey']}.
     *
     * @param topic        topic
     * @param payload      payload
     * @param partitionKey partition key
     * @throws IllegalArgumentException payload class is not one of those that {@link MessageTypeResolver} accepts in
     *                                  its {@code writeMessageType} methods
     */
    void publish(String topic, Object payload, @Nullable String partitionKey);

    /**
     * Publishes to the given topic.
     * <p>
     * NOTE: producer must be configured to borrow the partition key from {@code headers['partitionKey']}.
     *
     * @param topic        topic
     * @param payload      payload
     * @param partitionKey partition key
     * @param messageKey   message key (string)
     * @throws IllegalArgumentException payload class is not one of those that {@link MessageTypeResolver} accepts in
     *                                  its {@code writeMessageType} methods
     */
    void publish(String topic, Object payload, @Nullable String partitionKey, String messageKey);

    /**
     * Publishes payload to the given topic.
     * <p>
     * NOTE: producer must be configured to borrow the partition key from {@code headers['partitionKey']}.
     *
     * @param topic        topic
     * @param payload      payload
     * @param partitionKey partition key
     * @param messageKey   message key (bytes)
     * @throws IllegalArgumentException payload class is not one of those that {@link MessageTypeResolver} accepts in
     *                                  its {@code writeMessageType} methods
     */
    void publish(String topic, Object payload, @Nullable String partitionKey, @Nullable byte[] messageKey);

}
