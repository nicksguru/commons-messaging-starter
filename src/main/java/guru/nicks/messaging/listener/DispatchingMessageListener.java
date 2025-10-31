package guru.nicks.messaging.listener;

import guru.nicks.condition.ConditionalOnPropertyNotBlank;
import guru.nicks.log.domain.LogContext;
import guru.nicks.utils.ReflectionUtils;
import guru.nicks.validation.AnnotationValidator;

import am.ik.yavi.meta.ConstraintArguments;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ValidationException;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;

import static guru.nicks.validation.dsl.ValiDsl.checkNotNull;

/**
 * Dispatches messages to consumers based on {@link #findMessageConsumer(Message)} result. One message type may have
 * only one consumer within the same {@link #getId()}. Otherwise, if one consumer succeeds and another fails, the
 * message would be redelivered to ALL of them, which may cause side effects.
 * <p>
 * Consider annotating concrete listener beans with
 * {@code @ConditionalOnPropertyNotBlank("PREFIX.concreteListenerBeanNameSUFFIX")} (where PREFIX is
 * {@link #CONDITIONAL_PROPERTY_PREFIX}, and SUFFIX is {@link #CONDITIONAL_PROPERTY_SUFFIX}) to make them shared
 * components - even in some apps don't need them.
 * <p>
 */
@RequiredArgsConstructor
@Slf4j
public abstract class DispatchingMessageListener implements Consumer<Message<Map<String, Object>>> {

    /**
     * Prefix + listener bean name + suffix constitute the configuration property which, if not blank, enable the bean
     * creation.
     *
     * @see ConditionalOnPropertyNotBlank
     */
    public static final String CONDITIONAL_PROPERTY_PREFIX = "spring.cloud.stream.bindings.";
    public static final String CONDITIONAL_PROPERTY_SUFFIX = "-in-0.destination";

    @Getter
    @NonNull // Lombok creates runtime nullness check for this own annotation only
    private final String appName;

    @Getter
    @NonNull // Lombok creates runtime nullness check for this own annotation only
    private final List<MessageConsumer> allMessageConsumers;

    @Getter
    @NonNull // Lombok creates runtime nullness check for this own annotation only
    private final AnnotationValidator annotationValidator;

    @Getter
    @NonNull // Lombok creates runtime nullness check for this own annotation only
    private final ObjectMapper objectMapper;

    /**
     * Performs the following flow:
     * <ul>
     *     <li>stores {@link #getAppName()} in {@link LogContext#APP_NAME}</li>
     *     <li>stores {@link KafkaHeaders#RECEIVED_TOPIC} in {@link LogContext#MESSAGE_TOPIC}</li>
     *     <li>stores {@link MessageHeaders#ID} in {@link LogContext#MESSAGE_ID}</li>
     *     <li>logs message (what's exactly logged depends on its {@code toString()})</li>
     *     <li>find message consumer with {@link #findMessageConsumer(Message)}</li>
     *     <li>no consumer found - calls {@link #ignoreMessage(Message)}</li>
     *     <li>consumers found - calls {@link #consumeMessage(Message, MessageConsumer)} which, for each consumer:
     *          <ul>
     *              <li>deserializes message payload to {@link #getExpectedPayloadClass(MessageConsumer)}</li>
     *              <li>validates payload with {@link #validatePayload(Object)}</li>
     *              <li>passes control on to consumer's ({@link MessageConsumer#accept(Object, Object)})</li>
     *          </ul>
     *     </li>
     * </ul>
     *
     * @param message message
     */
    @Override
    public void accept(Message<Map<String, Object>> message) {
        try {
            LogContext.APP_NAME.put(appName);
            LogContext.MESSAGE_TOPIC.put(message.getHeaders().get(KafkaHeaders.RECEIVED_TOPIC));
            LogContext.MESSAGE_ID.put(message.getHeaders().get(MessageHeaders.ID));
            log.debug("Received message in listener [{}]: {}", getClass().getName(), message);

            findMessageConsumer(message).ifPresentOrElse(
                    consumer -> consumeMessage(message, consumer),
                    () -> ignoreMessage(message));
        } catch (RuntimeException e) {
            log.error("Message consumption failed, retrying; see DLQ if retries don't help. Reason: {}. Payload: {}.",
                    e.getMessage(), message.getPayload());
            throw e;
        }
    }

    /**
     * Returns this listener's unique ID (for example, the bean name) - it's referred to by message consumers as
     * {@link MessageConsumer#getMessageListenerId()}. This is needed to bind consumers to listeners in constructor.
     *
     * @return listener ID
     */
    public abstract String getId();

    /**
     * Finds a consumer for the given message. Why not multiple consumers? Because if one consumer succeeds and the
     * other fails, the message would be re-delivered to BOTH of them, which may cause side effects.
     *
     * @param message message
     * @return consumers, in undefined order
     */
    protected abstract Optional<MessageConsumer> findMessageConsumer(Message<Map<String, Object>> message);

    /**
     * Finds {@link #getAllMessageConsumers()} having {@link MessageConsumer#getMessageListenerId()} equal to
     * {@link #getId()}.
     *
     * @return message consumers bound to this message listener
     */
    protected List<MessageConsumer> findLinkedMessageConsumers() {
        checkNotNull(getId(), "listener ID should not be null");

        return allMessageConsumers.stream()
                .filter(consumer -> getId().equals(consumer.getMessageListenerId()))
                .toList();
    }

    /**
     * Passes message to the given consumer.
     *
     * @param message  message
     * @param consumer message consumers
     */
    @ConstraintArguments
    protected void consumeMessage(Message<Map<String, Object>> message, MessageConsumer consumer) {
        Object payload = objectMapper.convertValue(message.getPayload(), getExpectedPayloadClass(consumer));

        log.debug("Deserialized message in listener [{}] for consumer [{}]: {}",
                getClass().getName(), consumer.getClass().getName(), payload);

        validatePayload(payload);
        consumer.accept(payload, message.getHeaders());
    }

    /**
     * Reports message for which no consumer has been found, This is a frequent case if application only needs certain
     * messages: since it's subscribed to a topic, it receives all messages from that topic.
     *
     * @param message message
     */
    protected void ignoreMessage(Message<Map<String, Object>> message) {
        log.warn("No message consumer found in listener [{}] for {}", getClass().getName(), message.getPayload());
    }

    /**
     * Delegates to {@link AnnotationValidator#validate(Object)}.
     *
     * @param payload message payload
     * @throws ValidationException object is invalid ({@link ValidationException#getCause()} explains the details)
     */
    protected void validatePayload(Object payload) {
        annotationValidator.validate(payload);
    }

    /**
     * Returns class to deserialize message payload to.
     * <p>
     * WARNING: this method assumes that the class in question is the FIRST generic type in {@link MessageConsumer}.
     *
     * @return payload class
     * @throws NoSuchElementException class not found
     */
    protected Class<?> getExpectedPayloadClass(MessageConsumer<?> messageConsumer) {
        Class<?> payloadClass = ReflectionUtils
                .findFirstMaterializedGenericType(messageConsumer.getClass(), MessageConsumer.class)
                .orElseThrow(() -> new NoSuchElementException("Failed to guess message payload class for "
                        + messageConsumer.getClass().getName()));

        if (log.isTraceEnabled()) {
            log.trace("Message consumer [{}} expects payload class [{}]", messageConsumer.getClass().getName(),
                    payloadClass.getName());
        }

        return payloadClass;
    }

}
