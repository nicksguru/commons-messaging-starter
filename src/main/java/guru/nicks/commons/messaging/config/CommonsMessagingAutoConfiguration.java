package guru.nicks.commons.messaging.config;

import guru.nicks.commons.messaging.impl.KafkaMessagePublisherServiceImpl;
import guru.nicks.commons.messaging.service.MessagePublisherService;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@Slf4j
public class CommonsMessagingAutoConfiguration {

    /**
     * Creates {@link MessagePublisherService} bean if it's not already present.
     */
    @ConditionalOnMissingBean(MessagePublisherService.class)
    @Bean
    public MessagePublisherService messagePublisherService(StreamBridge streamBridge, ObjectMapper objectMapper) {
        log.debug("Building {} bean", MessagePublisherService.class.getSimpleName());
        return new KafkaMessagePublisherServiceImpl(streamBridge, objectMapper);
    }

}
