package guru.nicks.commons.messaging.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import guru.nicks.commons.messaging.impl.KafkaMessagePublisherServiceImpl;
import guru.nicks.commons.messaging.resolver.MessageTypeResolver;
import guru.nicks.commons.messaging.service.MessagePublisherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@Slf4j
public class CommonsMessagingAutoConfiguration {

    /**
     * Creates {@link MessagePublisherService} bean if it's not already present.
     */
    @ConditionalOnMissingBean(MessagePublisherService.class)
    @Bean
    public MessagePublisherService messagePublisherService(StreamBridge streamBridge,
            MessageTypeResolver messageTypeResolver, ObjectMapper objectMapper) {
        log.debug("Building {} bean", MessagePublisherService.class.getSimpleName());
        return new KafkaMessagePublisherServiceImpl(streamBridge, messageTypeResolver, objectMapper);
    }

}
