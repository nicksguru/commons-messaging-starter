package guru.nicks.messaging.listener;

import java.util.Map;

/**
 * Message consumer expecting a raw {@link Map} as payload.
 */
public interface RawMessageConsumer extends MessageConsumer<Map<String, Object>> {
}
