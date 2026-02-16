package guru.nicks.commons.messaging;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;

/**
 * A message that knows what its message key is, for Kafka.
 */
public interface MessageKeyAwareMessage {

    /**
     * Null means no partitioning. Can be set to userId etc. to ensure that the <b>same (partitioned) consumer</b>
     * processes <b>all events</b> having the same key. This is not the partition ID, this is a hint to Kafka to compute
     * it based on this value's hash.
     * <p>
     * This method must be configured in Spring Cloud Stream producer config as {@code payload.partitionKey}.
     */
    @JsonIgnore
    @Nullable
    String getMessageKey();

}
