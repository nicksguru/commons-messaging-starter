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
 * Uses headers as message type storage.
 */
public class HeaderBasedMessageTypeResolver implements MessageTypeResolver {

    @Getter
    private final String headerMessageTypeField;
    @Getter
    private final String payloadMessageTypeField;

    @ConstraintArguments
    public HeaderBasedMessageTypeResolver(String headerMessageTypeField, String payloadMessageTypeField) {
        this.headerMessageTypeField = checkNotBlank(headerMessageTypeField,
                _HeaderBasedMessageTypeResolverArgumentsMeta.HEADERMESSAGETYPEFIELD.name());
        this.payloadMessageTypeField = checkNotBlank(payloadMessageTypeField,
                _HeaderBasedMessageTypeResolverArgumentsMeta.PAYLOADMESSAGETYPEFIELD.name());
    }

    /**
     * Returns {@link #getHeaderMessageTypeField()} message header if it's not blank.
     */
    @Override
    public Optional<String> readMessageType(Message<Map<String, Object>> source) {
        return Optional.of(source.getHeaders())
                .map(map -> map.get(headerMessageTypeField))
                .map(Object::toString)
                .filter(StringUtils::isNotBlank);
    }

    /**
     * Copies {@link TypeAwareMessage#getType()} stringified with {@link MessageType#getMessageBrokerValue()} - if it's
     * not blank - to {@link #getHeaderMessageTypeField()} message header.
     */
    @Override
    public void writeMessageType(TypeAwareMessage<?> sourcePayload, Map<String, Object> targetPayload,
            Map<String, Object> targetHeaders) {
        Optional.ofNullable(sourcePayload.getType())
                .map(MessageType::getMessageBrokerValue)
                .filter(StringUtils::isNotBlank)
                .ifPresent(messageType -> targetHeaders.put(headerMessageTypeField, messageType));
    }

    /**
     * Copies {@link #getPayloadMessageTypeField()} source payload field - if it's not blank after
     * {@link Object#toString()} - to {@link #getHeaderMessageTypeField()} message header.
     */
    @Override
    public void writeMessageType(Map<String, Object> sourcePayload, Map<String, Object> targetPayload,
            Map<String, Object> targetHeaders) {
        Optional.ofNullable(sourcePayload.get(payloadMessageTypeField))
                .map(Object::toString)
                .filter(StringUtils::isNotBlank)
                .ifPresent(messageType -> targetHeaders.put(headerMessageTypeField, messageType));
    }

}
