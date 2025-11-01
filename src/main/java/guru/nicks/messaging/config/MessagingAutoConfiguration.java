package guru.nicks.messaging.config;

import guru.nicks.messaging.impl.KafkaMessagePublisherServiceImpl;
import guru.nicks.messaging.resolver.MessageTypeResolver;
import guru.nicks.messaging.service.MessagePublisherService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MessagingAutoConfiguration {

    @ConditionalOnMissingBean(MessagePublisherService.class)
    @Bean
    public MessagePublisherService messagePublisherService(StreamBridge streamBridge,
            MessageTypeResolver messageTypeResolver, ObjectMapper objectMapper) {
        return new KafkaMessagePublisherServiceImpl(streamBridge, messageTypeResolver, objectMapper);
    }

}
