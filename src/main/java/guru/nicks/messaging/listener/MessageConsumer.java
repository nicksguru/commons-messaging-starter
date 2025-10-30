package guru.nicks.messaging.listener;

import org.springframework.messaging.MessageHeaders;

import java.util.function.BiConsumer;

/**
 * Message consumer expecting a certain payload class. The expected message type (if
 * {@link #consumeUnknownMessageTypes()} is {@code false}) is inferred from the payload class (first argument to
 * {@link MessageConsumer#accept(Object, Object)}).
 * <p>
 * WARNING: one message type may have only one consumer within the same {@link #getMessageListenerBeanName()}.
 * Otherwise, if one consumer succeeds and another fails, the message would be redelivered to ALL of them, which may
 * cause side effects.
 *
 * @param <P> payload type
 */
public interface MessageConsumer<P> extends BiConsumer<P, MessageHeaders> {

    /**
     * {@link DispatchingMessageListener} beans only see consumers referring to their bean name and dispatch messages to
     * them.
     */
    String getMessageListenerBeanName();

    /**
     * If {@code true}, this consumer is called by the bound listener for:
     * <ol>
     *     <li>all messages having no type detected</li>
     *     <li>all messages having types that have no consumers bound to them explicitly</li>
     * </ol>
     *
     * @return default implementation returns {@code false}
     */
    default boolean consumeUnknownMessageTypes() {
        return false;
    }

}
