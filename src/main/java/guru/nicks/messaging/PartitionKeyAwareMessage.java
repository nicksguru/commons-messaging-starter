package guru.nicks.messaging;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;

/**
 * Spring Cloud Stream Kafka must be configured so that {@link #getPartitionKey()} affects target topic partition.
 */
public interface PartitionKeyAwareMessage {

    /**
     * Null means no partitioning. Can be set to userId etc. to ensure that the <b>same</b> (partitioned) consumer
     * processes <b>all</b> events having the same key. This is not the partition ID, this is a hint to compute it based
     * on this value's hash and the number of partitions.
     * <p>
     * This method must be configured in Spring Cloud Stream producer config as {@code payload.partitionKey}.
     */
    @JsonIgnore
    @Nullable
    String getPartitionKey();

}
