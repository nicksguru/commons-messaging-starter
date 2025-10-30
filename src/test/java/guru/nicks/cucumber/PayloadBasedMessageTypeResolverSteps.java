package guru.nicks.cucumber;

import guru.nicks.cucumber.world.TextWorld;
import guru.nicks.messaging.MessageType;
import guru.nicks.messaging.TypeAwareMessage;
import guru.nicks.messaging.resolver.PayloadBasedMessageTypeResolver;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.DataTableType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor
public class PayloadBasedMessageTypeResolverSteps {

    // DI
    private final TextWorld textWorld;

    @Mock
    private TypeAwareMessage<MessageType> typeAwareMessage;
    @Mock
    private MessageType messageType;
    private AutoCloseable closeableMocks;

    private PayloadBasedMessageTypeResolver resolver;
    private Message<Map<String, Object>> message;
    private Map<String, Object> sourcePayload;
    private Map<String, Object> targetPayload;
    private Map<String, Object> targetHeaders;
    private Optional<String> result;

    @Before
    public void beforeEachScenario() {
        closeableMocks = MockitoAnnotations.openMocks(this);
    }

    @After
    public void afterEachScenario() throws Exception {
        closeableMocks.close();
    }

    @DataTableType
    public MessageData createMessageData(Map<String, String> entry) {
        return MessageData.builder()
                .fieldName(entry.get("fieldName"))
                .fieldValue(entry.get("fieldValue"))
                .expectedResult(entry.get("expectedResult"))
                .build();
    }

    @Given("a PayloadBasedMessageTypeResolver is initialized with field name {string}")
    public void aPayloadBasedMessageTypeResolverIsInitializedWithFieldName(String fieldName) {
        resolver = new PayloadBasedMessageTypeResolver(fieldName);
    }

    @Given("a message with payload containing {string} with value {string}")
    public void aMessageWithPayloadContainingWithValue(String fieldName, String fieldValue) {
        Map<String, Object> payload = new HashMap<>();

        if (!"null".equals(fieldValue)) {
            payload.put(fieldName, fieldValue);
        }

        message = new GenericMessage<>(payload);
    }

    @Given("a message with empty payload")
    public void aMessageWithEmptyPayload() {
        message = new GenericMessage<>(Collections.emptyMap());
    }

    @Given("a TypeAwareMessage with type {string}")
    public void aTypeAwareMessageWithType(String messageTypeValue) {
        if ("null".equals(messageTypeValue)) {
            when(typeAwareMessage.getType())
                    .thenReturn(null);
        } else {
            when(typeAwareMessage.getType())
                    .thenReturn(messageType);
            when(messageType.getMessageBrokerValue())
                    .thenReturn(messageTypeValue);
        }
    }

    @Given("an empty target payload")
    public void anEmptyTargetPayload() {
        targetPayload = new HashMap<>();
    }

    @Given("an empty target headers map")
    public void anEmptyTargetHeadersMap() {
        targetHeaders = new HashMap<>();
    }

    @Given("a source payload with {string} set to {string}")
    public void aSourcePayloadWithSetTo(String fieldName, String fieldValue) {
        sourcePayload = new HashMap<>();
        if (!"null".equals(fieldValue)) {
            sourcePayload.put(fieldName, fieldValue);
        }
    }

    @Given("message properties with blank payload message type field")
    public void messagePropertiesWithBlankPayloadMessageTypeField() {
        resolver = new PayloadBasedMessageTypeResolver(null);
    }

    @When("the message type is read from the message payload")
    public void theMessageTypeIsReadFromMessagePayload() {
        result = resolver.readMessageType(message);
    }

    @When("the message type is written from TypeAwareMessage to payload")
    public void theMessageTypeIsWrittenFromTypeAwareMessageToPayload() {
        resolver.writeMessageType(typeAwareMessage, targetPayload, targetHeaders);
    }

    @When("the message type is written from source payload to target payload")
    public void theMessageTypeIsWrittenFromSourcePayloadToTargetPayload() {
        resolver.writeMessageType(sourcePayload, targetPayload, targetHeaders);
    }

    @When("the PayloadBasedMessageTypeResolver is initialized with blank payload message type field")
    public void thePayloadBasedMessageTypeResolverIsInitializedWithBlankPayloadMessageTypeField() {
        textWorld.setLastException(catchThrowable(() ->
                resolver = new PayloadBasedMessageTypeResolver("")));
    }

    @Then("the result should contain {string}")
    public void theResultShouldContain(String expectedValue) {
        if (expectedValue.isEmpty()) {
            assertThat(result)
                    .as("result")
                    .isEmpty();
        } else {
            assertThat(result)
                    .as("result")
                    .isPresent()
                    .contains(expectedValue);
        }
    }

    @Then("the result should be empty")
    public void theResultShouldBeEmpty() {
        assertThat(result)
                .as("result")
                .isEmpty();
    }

    @Then("the target payload should contain key {string} with value {string}")
    public void theTargetPayloadShouldContainKeyWithValue(String key, String expectedValue) {
        if (expectedValue.isEmpty()) {
            assertThat(targetPayload.containsKey(key))
                    .as("target payload contains key")
                    .isFalse();
        } else {
            assertThat(targetPayload)
                    .as("target payload")
                    .containsEntry(key, expectedValue);
        }
    }

    @Value
    @Builder
    public static class MessageData {

        String fieldName;
        String fieldValue;
        String expectedResult;

    }

}
