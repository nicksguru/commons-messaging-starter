package guru.nicks.cucumber;

import guru.nicks.messaging.MessageType;
import guru.nicks.messaging.TypeAwareMessage;
import guru.nicks.messaging.resolver.HeaderBasedMessageTypeResolver;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.Builder;
import lombok.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HeaderBasedMessageTypeResolverSteps {

    private HeaderBasedMessageTypeResolver resolver;
    private Message<Map<String, Object>> message;
    private Optional<String> result;

    private Map<String, Object> targetPayload;
    private Map<String, Object> targetHeaders;
    private Map<String, Object> sourcePayload;
    private TestTypeAwareMessage typeAwareMessage;

    @Before
    public void beforeEachScenario() {
        resolver = new HeaderBasedMessageTypeResolver("message-type", "messageType");

        targetPayload = new HashMap<>();
        targetHeaders = new HashMap<>();
        sourcePayload = new HashMap<>();
    }

    @Given("a message with header {string} set to {string}")
    public void aMessageWithHeaderSetTo(String headerName, String headerValue) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(headerName, headerValue);

        Map<String, Object> payload = new HashMap<>();
        message = mock(Message.class);

        when(message.getHeaders())
                .thenReturn(new MessageHeaders(headers));
        when(message.getPayload())
                .thenReturn(payload);
    }

    @When("the message type is read from the message headers")
    public void theMessageTypeIsReadFromMessageHeaders() {
        result = resolver.readMessageType(message);
    }

    @Then("the message type should be {string}")
    public void theMessageTypeShouldBe(String expectedType) {
        if (expectedType.isEmpty()) {
            assertThat(result)
                    .as("result")
                    .isEmpty();
        } else {
            assertThat(result)
                    .as("result value")
                    .contains(expectedType);
        }
    }

    @Given("a type aware message with type {string}")
    public void aTypeAwareMessageWithType(String messageType) {
        typeAwareMessage = TestTypeAwareMessage.builder()
                .type(messageType.isEmpty() ? null : new TestMessageType(messageType))
                .build();
    }

    @Given("empty target payload and headers")
    public void emptyTargetPayloadAndHeaders() {
        targetPayload = new HashMap<>();
        targetHeaders = new HashMap<>();
    }

    @When("the message type is written to the target")
    public void theMessageTypeIsWrittenToTheTarget() {
        resolver.writeMessageType(typeAwareMessage, targetPayload, targetHeaders);
    }

    @Then("the target header {string} should be {string}")
    public void theTargetHeaderShouldBe(String headerName, String expectedValue) {
        if (expectedValue.isEmpty()) {
            assertThat(targetHeaders.containsKey(headerName))
                    .as("target headers contains key " + headerName)
                    .isFalse();
        } else {
            assertThat(targetHeaders)
                    .as("target headers")
                    .containsEntry(headerName, expectedValue);
        }
    }

    @Given("a source payload with field {string} set to {string}")
    public void aSourcePayloadWithFieldSetTo(String fieldName, String fieldValue) {
        sourcePayload = new HashMap<>();

        if (!fieldValue.isEmpty()) {
            sourcePayload.put(fieldName, fieldValue);
        }
    }

    @When("the message type is written from source payload to target")
    public void theMessageTypeIsWrittenFromSourcePayloadToTarget() {
        resolver.writeMessageType(sourcePayload, targetPayload, targetHeaders);
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
