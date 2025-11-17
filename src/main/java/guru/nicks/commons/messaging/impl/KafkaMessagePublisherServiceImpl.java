package guru.nicks.commons.messaging.impl;

import guru.nicks.commons.messaging.TypeAwareMessage;
import guru.nicks.commons.messaging.resolver.MessageTypeResolver;
import guru.nicks.commons.messaging.service.MessagePublisherService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


/**
 * Publishes messages to Kafka topics using Spring Cloud Stream.
 */
@RequiredArgsConstructor
@Slf4j
public class KafkaMessagePublisherServiceImpl implements MessagePublisherService {

    /**
     * DI. Ideally this should be {@code message.setHeader("spring.cloud.stream.sendto.destination", topic)}, but it
     * works in functional style listeners only. Here, {@link StreamBridge#send(String, Object)} is used instead.
     */
    private final StreamBridge streamBridge;
    private final MessageTypeResolver messageTypeResolver;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(String topic, Object payload, @Nullable String partitionKey) {
        publishInternal(topic, payload, partitionKey, null);
    }

    @Override
    public void publish(String topic, Object payload, @Nullable String partitionKey, @Nullable String messageKey) {
        // convert to bytes to avoid: 'org.apache.kafka.common.errors.SerializationException: Can't convert key of class
        // class java.lang.String to class org.apache.kafka.common.serialization.ByteArraySerializer specified in
        // key.serializer'
        byte[] messageKeyBytes = (messageKey == null) ? null : messageKey.getBytes(StandardCharsets.UTF_8);
        publishInternal(topic, payload, partitionKey, messageKeyBytes);
    }

    @Override
    public void publish(String topic, Object payload, @Nullable String partitionKey, @Nullable byte[] messageKey) {
        publishInternal(topic, payload, partitionKey, messageKey);
    }

    private void publishInternal(String topic, Object payload,
            @Nullable String partitionKey, @Nullable byte[] messageKey) {
        // avoid unchecked map assignment (TypeReference instead of Map.class)
        Map<String, Object> payloadAsMap = objectMapper.convertValue(payload, new TypeReference<>() {
        });

        // MessageHeaders are immutable, which doesn't fit here
        var headers = new HashMap<String, Object>();
        // Spring Cloud Stream complains if its partitionKeyExpression yields null value, hence '' as fallback
        headers.put("partitionKey", Optional.ofNullable(partitionKey).orElse(""));

        if (messageKey != null) {
            headers.put(KafkaHeaders.KEY, messageKey);
        }

        // retrieve message type out of known classes
        switch (payload) {
            case TypeAwareMessage<?> typeAwareMessage:
                messageTypeResolver.writeMessageType(typeAwareMessage, payloadAsMap, headers);
                break;

            case Map map:
                messageTypeResolver.writeMessageType(map, payloadAsMap, headers);
                break;

            default:
                throw new IllegalArgumentException("Payload class not supported by ["
                        + messageTypeResolver.getClass().getName()
                        + "]: " + payload.getClass());
        }

        Message<Map<String, Object>> message = new GenericMessage<>(payloadAsMap, headers);
        log.info("Publishing to topic '{}': {}", topic, message);
        streamBridge.send(topic, message);
    }

}
