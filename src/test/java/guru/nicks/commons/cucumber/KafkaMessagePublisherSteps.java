package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.messaging.MessageType;
import guru.nicks.commons.messaging.TypeAwareMessage;
import guru.nicks.commons.messaging.impl.KafkaMessagePublisherServiceImpl;
import guru.nicks.commons.messaging.impl.NoOpMessageTypeResolver;
import guru.nicks.commons.messaging.service.MessagePublisherService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor
public class KafkaMessagePublisherSteps {

    // DI
    private final TextWorld textWorld;

    @Mock
    private StreamBridge streamBridge;
    @Mock
    private ObjectMapper objectMapper;
    @Captor
    private ArgumentCaptor<Message<Map<String, Object>>> messageCaptor;
    @Captor
    private ArgumentCaptor<String> topicCaptor;
    private AutoCloseable closeableMocks;

    private MessagePublisherService publisherService;
    private String topic;
    private String payloadType;
    private String messageKeyString;
    private byte[] messageKeyBytes;

    private Object payload;
    private Map<String, Object> payloadAsMap;

    @Before
    public void beforeEachScenario() {
        closeableMocks = MockitoAnnotations.openMocks(this);
        publisherService = new KafkaMessagePublisherServiceImpl(streamBridge, objectMapper);
        payloadAsMap = new HashMap<>();

        when(objectMapper.convertValue(any(), any(TypeReference.class)))
                .thenReturn(payloadAsMap);

        // mock message publisher
        when(streamBridge.send(any(String.class), any(Message.class)))
                .thenReturn(true);
    }

    @After
    public void afterEachScenario() throws Exception {
        closeableMocks.close();
    }

    @Given("a message with topic {string} and payload type {string}")
    public void aMessageWithTopicAndPayloadType(String topic, String payloadType) {
        this.topic = topic;
        this.payloadType = payloadType;
    }

    @Given("the payload has message type {string}")
    public void thePayloadHasMessageType(String messageType) {
        switch (payloadType) {
            case "TypeAwareMessage" -> payload = TestTypeAwareMessage.builder()
                    .type(new TestMessageType(messageType))
                    .build();
            case "Map" -> {
                var map = new HashMap<String, Object>();
                map.put("messageType", messageType);
                payload = map;
            }
            case "String" -> payload = "This is a string payload";
            default -> throw new IllegalArgumentException("Unsupported payload type: '" + payloadType + "'");
        }
    }

    @Given("the message key is {string}")
    public void theMessageKeyIs(String messageKey) {
        this.messageKeyString = messageKey;
    }

    @Given("the message key as bytes is {string}")
    public void theMessageKeyAsBytesIs(String messageKey) {
        if (messageKey != null && !messageKey.isEmpty()) {
            this.messageKeyBytes = messageKey.getBytes(StandardCharsets.UTF_8);
        }
    }

    @When("the message is published")
    public void theMessageIsPublished() {
        textWorld.setLastException(catchThrowable(() ->
                publisherService.publish(topic, payload, null, NoOpMessageTypeResolver.INSTANCE)
        ));
    }

    @When("the message is published with string key")
    public void theMessageIsPublishedWithStringKey() {
        textWorld.setLastException(catchThrowable(() ->
                publisherService.publish(topic, payload, messageKeyString, NoOpMessageTypeResolver.INSTANCE)
        ));
    }

    @When("the message is published with byte key")
    public void theMessageIsPublishedWithByteKey() {
        textWorld.setLastException(catchThrowable(() ->
                publisherService.publish(topic, payload, messageKeyBytes, NoOpMessageTypeResolver.INSTANCE)
        ));
    }

    @Then("the message should be sent to the topic")
    public void theMessageShouldBeSentToTheTopic() {
        verify(streamBridge).send(topicCaptor.capture(), messageCaptor.capture());

        assertThat(topicCaptor.getValue())
                .as("topic")
                .isEqualTo(topic);
    }

    @Then("the message headers should contain the message key")
    public void theMessageHeadersShouldContainTheMessageKey() {
        var headers = messageCaptor.getValue().getHeaders();

        assertThat(headers)
                .as("headers")
                .containsKey(KafkaHeaders.KEY);

        var actualKeyBytes = (byte[]) headers.get(KafkaHeaders.KEY);
        var expectedKeyBytes = messageKeyString.getBytes(StandardCharsets.UTF_8);

        assertThat(actualKeyBytes)
                .as("message key bytes")
                .isEqualTo(expectedKeyBytes);
    }

    @Then("the message headers should contain the message key as bytes")
    public void theMessageHeadersShouldContainTheMessageKeyAsBytes() {
        var headers = messageCaptor.getValue().getHeaders();

        if (messageKeyBytes == null) {
            assertThat(headers)
                    .as("headers")
                    .doesNotContainKey(KafkaHeaders.KEY);
        } else {
            assertThat(headers)
                    .as("headers")
                    .containsKey(KafkaHeaders.KEY);

            var actualKeyBytes = (byte[]) headers.get(KafkaHeaders.KEY);

            assertThat(actualKeyBytes)
                    .as("message key bytes")
                    .isEqualTo(messageKeyBytes);
        }
    }

    @Value
    private static class TestMessageType implements MessageType {

        String value;

        @Override
        public String getMessageBrokerValue() {
            return value;
        }

    }

    @Value
    @Builder
    private static class TestTypeAwareMessage implements TypeAwareMessage<MessageType> {
        MessageType type;

        @Override
        public MessageType getType() {
            return type;
        }

    }

}
