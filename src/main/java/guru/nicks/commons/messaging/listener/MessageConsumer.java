package guru.nicks.commons.messaging.listener;

import org.springframework.messaging.MessageHeaders;

import java.util.function.BiConsumer;

/**
 * Message consumer expecting a certain payload class.
 * <p>
 * WARNING: one message type may have only one consumer within the same {@link #getMessageListenerId()}. Otherwise, if
 * one consumer succeeds and the other fails, the message would be re-delivered to BOTH of them, which may cause side
 * effects.
 *
 * @param <P> payload type
 */
public interface MessageConsumer<P> extends BiConsumer<P, MessageHeaders> {

    /**
     * Each {@link DispatchingMessageListener} only dispatches messages to consumers referring to their ID.
     */
    String getMessageListenerId();

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
