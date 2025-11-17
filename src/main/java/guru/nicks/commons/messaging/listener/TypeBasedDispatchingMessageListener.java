package guru.nicks.commons.messaging.listener;

import guru.nicks.commons.messaging.MessageType;
import guru.nicks.commons.messaging.TypeAwareMessage;
import guru.nicks.commons.messaging.resolver.MessageTypeResolver;
import guru.nicks.commons.utils.ReflectionUtils;
import guru.nicks.commons.validation.AnnotationValidator;

import am.ik.yavi.meta.ConstraintArguments;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSortedMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static guru.nicks.commons.messaging.TypeAwareMessage.UNKNOWN_MESSAGE_TYPE;
import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;
import static java.util.function.Predicate.not;

/**
 * Dispatches messages to consumers based on {@link MessageTypeResolver#readMessageType(Message)}.
 * <p>
 * Finds message consumers by their {@link TypeAwareMessage#getType()}.
 */
@Slf4j
public abstract class TypeBasedDispatchingMessageListener extends DispatchingMessageListener
        implements MessageTypeResolver {

    /**
     * Each key is {@link #getExpectedMessageType(MessageConsumer)}.
     */
    @Getter(AccessLevel.PROTECTED)
    private final Map<String, MessageConsumer> messageTypeToConsumer;

    @Getter(AccessLevel.PROTECTED)
    private final MessageTypeResolver messageTypeResolver;

    @ConstraintArguments
    protected TypeBasedDispatchingMessageListener(String appName,
            List<MessageConsumer> allMessageConsumers, MessageTypeResolver messageTypeResolver,
            AnnotationValidator annotationValidator, ObjectMapper objectMapper) {
        super(appName, allMessageConsumers, annotationValidator, objectMapper);

        this.messageTypeResolver = checkNotNull(messageTypeResolver,
                _TypeBasedDispatchingMessageListenerArgumentsMeta.MESSAGETYPERESOLVER.name());
        if (messageTypeResolver == this) {
            log.warn("{} is self-referential (this listener implements the interface)",
                    MessageTypeResolver.class.getSimpleName());
        }

        var tmpMessageTypeToConsumer = new TreeMap<String, MessageConsumer>();

        findLinkedMessageConsumers().forEach(consumer -> {
            String messageType = getExpectedMessageType(consumer);

            if (tmpMessageTypeToConsumer.containsKey(messageType)) {
                throw new IllegalStateException("Can't bind message consumer [" + consumer.getClass().getName() + "]: "
                        + "message type '" + messageType + "' is already bound to consumer ["
                        + tmpMessageTypeToConsumer.get(messageType).getClass().getName() + "]");
            }

            tmpMessageTypeToConsumer.put(messageType, consumer);
        });

        // immutability and sorting (for readability)
        messageTypeToConsumer = ImmutableSortedMap.copyOf(tmpMessageTypeToConsumer);

        // no exception, though
        if (messageTypeToConsumer.isEmpty()) {
            log.warn("Message listener [{}] accepts messages, but doesn't dispatch them: no consumers registered",
                    getClass().getName());
        } else {
            logRegisteredMessageConsumers();
        }
    }

    /**
     * Finds message consumer(s) based on {@link #readMessageType(Message)}. If no consumers are found, falls back on
     * those bound to {@link TypeAwareMessage#UNKNOWN_MESSAGE_TYPE}.
     *
     * @param message message
     * @return consumers, possibly empty
     */
    @Override
    protected Optional<MessageConsumer> findMessageConsumer(Message<Map<String, Object>> message) {
        String messageType = readMessageType(message).orElse(UNKNOWN_MESSAGE_TYPE);
        MessageConsumer consumer = messageTypeToConsumer.get(messageType);

        if (consumer == null) {
            consumer = messageTypeToConsumer.get(UNKNOWN_MESSAGE_TYPE);
        }

        return Optional.ofNullable(consumer);
    }

    /**
     * Delegates to {@link MessageTypeResolver}.
     */
    @ConstraintArguments
    @Override
    public Optional<String> readMessageType(Message<Map<String, Object>> source) {
        checkNotNull(source, _TypeBasedDispatchingMessageListenerReadMessageTypeArgumentsMeta.SOURCE.name());
        return messageTypeResolver.readMessageType(source);
    }

    /**
     * Delegates to {@link MessageTypeResolver#writeMessageType(TypeAwareMessage, Map, Map)}.
     */
    @ConstraintArguments
    @Override
    public void writeMessageType(TypeAwareMessage<?> sourcePayload, Map<String, Object> targetPayload,
            Map<String, Object> targetHeaders) {
        checkNotNull(sourcePayload,
                _TypeBasedDispatchingMessageListenerWriteMessageTypeArgumentsMeta.SOURCEPAYLOAD.name());
        messageTypeResolver.writeMessageType(sourcePayload, targetPayload, targetHeaders);
    }

    /**
     * Delegates to {@link MessageTypeResolver#writeMessageType(Map, Map, Map)}.
     */
    @Override
    public void writeMessageType(Map<String, Object> sourcePayload, Map<String, Object> targetPayload,
            Map<String, Object> targetHeaders) {
        messageTypeResolver.writeMessageType(sourcePayload, targetPayload, targetHeaders);
    }

    /**
     * Pretty-prints {@link #getMessageTypeToConsumer()}.
     */
    protected void logRegisteredMessageConsumers() {
        String formattedMessageConsumers = messageTypeToConsumer.entrySet()
                .stream()
                .map(mapEntry ->
                        // message type: if not 'unknown', wrap in quotes
                        Optional.of(mapEntry.getKey())
                                .filter(not(UNKNOWN_MESSAGE_TYPE::equals))
                                .map(messageType -> "'" + messageType + "'")
                                .orElse("<UNKNOWN/UNBOUND>")
                                + " -> "
                                // consumer bound to this message type
                                + mapEntry.getValue().getClass().getName()
                                + "[payload: " + getExpectedPayloadClass(mapEntry.getValue()).getName() + "]")
                // join message types
                .collect(Collectors.joining("; "));

        log.info("Message listener [{}] dispatches {} message type{}: {}", getClass().getName(),
                messageTypeToConsumer.size(), (messageTypeToConsumer.size() == 1) ? "" : "s",
                formattedMessageConsumers);
    }

    /**
     * Returns message type the given consumer is bound to:
     * <ul>
     *  <li>if {@link MessageConsumer#consumeUnknownMessageTypes()} is {@code true}, it's
     *      {@value TypeAwareMessage#UNKNOWN_MESSAGE_TYPE}</li>
     *  <li>otherwise {@link MessageType#getMessageBrokerValue()} is called on the payload object instantiated for that
     *      purpose (its class must inherit from {@link TypeAwareMessage} in that case)</li>
     * </ul>
     *
     * @param messageConsumer message consumer
     * @return message type the consumer expects
     * @throws IllegalStateException if payload class doesn't inherit from {@link TypeAwareMessage} or can't be
     *                               instantiated
     */
    protected String getExpectedMessageType(MessageConsumer<?> messageConsumer) {
        if (messageConsumer.consumeUnknownMessageTypes()) {
            return UNKNOWN_MESSAGE_TYPE;
        }

        Class<?> payloadClass = getExpectedPayloadClass(messageConsumer);

        if (!TypeAwareMessage.class.isAssignableFrom(payloadClass)) {
            throw new IllegalStateException("Payload class must inherit from " + TypeAwareMessage.class.getName()
                    + ": " + payloadClass.getName());
        }

        // it's OK to have empty message type (which by design means unknown/unbound), but never null
        TypeAwareMessage<?> payload;
        try {
            payload = (TypeAwareMessage<?>) ReflectionUtils.instantiateEvenWithoutDefaultConstructor(payloadClass);
        } catch (Exception e) {
            throw new IllegalStateException(String.format(
                    "Failed to instantiate payload class [%s] for message type extraction: %s",
                    payloadClass.getName(), e.getMessage()), e);
        }

        checkNotNull(payload.getType(), payloadClass.getName() + ".type");

        @SuppressWarnings("java:S1488") // redundant local variable, for debugging
        String messageType = checkNotNull(payload.getType().getMessageBrokerValue(),
                payloadClass.getName() + ".type.messageBrokerValue");
        return messageType;
    }

}
