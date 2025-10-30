package guru.nicks.messaging.resolver;

import guru.nicks.messaging.MessageType;
import guru.nicks.messaging.TypeAwareMessage;

import am.ik.yavi.meta.ConstraintArguments;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.Message;

import java.util.Map;
import java.util.Optional;

import static guru.nicks.validation.dsl.ValiDsl.checkNotBlank;

/**
 * Uses message payload as message type storage.
 */
public class PayloadBasedMessageTypeResolver implements MessageTypeResolver {

    @Getter
    private final String payloadMessageTypeField;

    @ConstraintArguments
    public PayloadBasedMessageTypeResolver(String payloadMessageTypeField) {
        this.payloadMessageTypeField = checkNotBlank(payloadMessageTypeField,
                _PayloadBasedMessageTypeResolverArgumentsMeta.PAYLOADMESSAGETYPEFIELD.name());
    }

    /**
     * Returns {@link #getPayloadMessageTypeField()} payload field value if it's not blank.
     */
    @Override
    public Optional<String> readMessageType(Message<Map<String, Object>> source) {
        return Optional.ofNullable(source.getPayload())
                .map(map -> map.get(payloadMessageTypeField))
                .map(Object::toString)
                .filter(StringUtils::isNotBlank);
    }

    /**
     * Copies {@link TypeAwareMessage#getType()} stringified with {@link MessageType#getMessageBrokerValue()} - if it's
     * not blank - to {@link #getPayloadMessageTypeField()} target payload field.
     */
    @Override
    public void writeMessageType(TypeAwareMessage<?> sourcePayload, Map<String, Object> targetPayload,
            Map<String, Object> targetHeaders) {
        Optional.ofNullable(sourcePayload.getType())
                .map(MessageType::getMessageBrokerValue)
                .filter(StringUtils::isNotBlank)
                .ifPresent(messageType -> targetPayload.put(payloadMessageTypeField, messageType));
    }

    /**
     * Copies {@link #getPayloadMessageTypeField()} source payload field - if it's not blank after
     * {@link Object#toString()} - to same-named target payload field.
     */
    @Override
    public void writeMessageType(Map<String, Object> sourcePayload, Map<String, Object> targetPayload,
            Map<String, Object> targetHeaders) {
        Optional.ofNullable(sourcePayload.get(payloadMessageTypeField))
                .map(Object::toString)
                .filter(StringUtils::isNotBlank)
                .ifPresent(messageType -> targetPayload.put(payloadMessageTypeField, messageType));
    }

}
