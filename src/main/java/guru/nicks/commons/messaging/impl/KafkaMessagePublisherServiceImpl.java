package guru.nicks.commons.messaging.impl;

import guru.nicks.commons.messaging.TypeAwareMessage;
import guru.nicks.commons.messaging.resolver.MessageTypeResolver;
import guru.nicks.commons.messaging.service.MessagePublisherService;
import guru.nicks.commons.utils.json.JsonUtils;

import am.ik.yavi.meta.ConstraintArguments;
import com.fasterxml.jackson.core.JsonProcessingException;
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

import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotBlank;
import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;


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
    private final ObjectMapper objectMapper;

    @Override
    public void publish(String topic, Object payload, @Nullable Object messageKey,
            MessageTypeResolver messageTypeResolver) {
        // convert to bytes to avoid: 'org.apache.kafka.common.errors.SerializationException: Can't convert key of class
        // class java.lang.String to class org.apache.kafka.common.serialization.ByteArraySerializer specified in
        // key.serializer'
        byte[] messageKeyBytes = null;

        if (messageKey != null) {
            String str = messageKey.toString();

            if (str != null) {
                messageKeyBytes = str.getBytes(StandardCharsets.UTF_8);
            }
        }

        publishInternal(topic, payload, messageKeyBytes, messageTypeResolver);
    }

    @Override
    public void publish(String topic, Object payload, @Nullable byte[] messageKey,
            MessageTypeResolver messageTypeResolver) {
        publishInternal(topic, payload, messageKey, messageTypeResolver);
    }

    @ConstraintArguments
    private void publishInternal(String topic, Object payload, @Nullable byte[] messageKey,
            MessageTypeResolver messageTypeResolver) {
        checkNotBlank(topic, _KafkaMessagePublisherServiceImplPublishInternalArgumentsMeta.TOPIC.name());
        checkNotNull(payload, _KafkaMessagePublisherServiceImplPublishInternalArgumentsMeta.PAYLOAD.name());

        // avoid unchecked map assignment (TypeReference instead of Map.class)
        Map<String, Object> payloadAsMap = objectMapper.convertValue(payload, new TypeReference<>() {
        });

        // WARNING: MessageHeaders are immutable, but MessageTypeResolver writes to headers/payload, therefore
        // both payloadAsMap and headersAsMap MUST be mutable
        Map<String, Object> headersAsMap = HashMap.newHashMap(2);
        if (messageKey != null) {
            headersAsMap.put(KafkaHeaders.KEY, messageKey);
        }

        // retrieve message type out of known classes
        switch (payload) {
            case TypeAwareMessage<?> typeAwareMessage:
                messageTypeResolver.writeMessageType(typeAwareMessage, payloadAsMap, headersAsMap);
                break;

            case Map map:
                messageTypeResolver.writeMessageType(map, payloadAsMap, headersAsMap);
                break;

            default:
                throw new IllegalArgumentException("Payload class not supported by ["
                        + messageTypeResolver.getClass().getName()
                        + "]: " + payload.getClass());
        }

        Message<Map<String, Object>> message = new GenericMessage<>(payloadAsMap, headersAsMap);

        // don't log raw payload, rather mask it (convert to JSON first)
        try {
            String json = objectMapper.writeValueAsString(message);
            log.debug("Publishing to topic '{}': {}", topic, JsonUtils.maskSensitiveJsonFields(json));
        } catch (JsonProcessingException ex) {
            log.warn("Publishing to topic '{}' (can't serialize message to masked JSON for logging)", topic);
        }

        streamBridge.send(topic, message);
    }

}
