package guru.nicks.cucumber;

import guru.nicks.cucumber.world.TextWorld;
import guru.nicks.messaging.MessageType;
import guru.nicks.messaging.TypeAwareMessage;
import guru.nicks.messaging.listener.MessageConsumer;
import guru.nicks.messaging.listener.TypeBasedDispatchingMessageListener;
import guru.nicks.messaging.resolver.MessageTypeResolver;
import guru.nicks.validation.AnnotationValidator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Step definitions for testing the {@link TypeBasedDispatchingMessageListener} implementation.
 */
@Slf4j
@RequiredArgsConstructor
public class TypeBasedDispatchingMessageListenerSteps {

    // DI
    private final TextWorld textWorld;
    private final List<MessageConsumer> messageConsumers = new ArrayList<>();
    @Mock
    private MessageTypeResolver mockMessageTypeResolver;
    @Mock
    private AnnotationValidator annotationValidator;
    private AutoCloseable closeableMocks;
    private TestTypeBasedDispatchingMessageListener listener;
    private Message<Map<String, Object>> testMessage;

    private boolean consumerCalled;
    private boolean unknownTypeConsumerCalled;

    @Before
    public void beforeEachScenario() {
        closeableMocks = MockitoAnnotations.openMocks(this);
        consumerCalled = false;
        unknownTypeConsumerCalled = false;
    }

    @After
    public void afterEachScenario() throws Exception {
        closeableMocks.close();
    }

    @Given("a type based dispatching message listener is initialized")
    public void aTypeBasedDispatchingMessageListenerIsInitialized() {
        doNothing().when(annotationValidator).validate(any());
        textWorld.setLastException(catchThrowable(() ->
                listener = new TestTypeBasedDispatchingMessageListener(mockMessageTypeResolver)));
    }

    @Given("a type based dispatching message listener is initialized with no consumers")
    public void aTypeBasedDispatchingMessageListenerIsInitializedWithNoConsumers() {
        listener = new TestTypeBasedDispatchingMessageListener(mockMessageTypeResolver, true);
    }

    @Given("a message consumer is registered for test message")
    public void aMessageConsumerIsRegisteredForTestMessage() {
        MessageConsumer<TestTypeAwareMessage> testConsumer = new TestMessageConsumer(false);
        addMessageConsumer(testConsumer);
    }

    @Given("a message consumer is registered for unknown message types")
    public void aMessageConsumerIsRegisteredForUnknownMessageTypes() {
        MessageConsumer<TestTypeAwareMessage> unknownTypeConsumer = new TestMessageConsumer(true);
        addMessageConsumer(unknownTypeConsumer);
    }

    @Given("a message type resolver that resolves message type to {string}")
    public void aMessageTypeResolverThatResolvesMessageTypeTo(String messageType) {
        when(mockMessageTypeResolver.readMessageType(any()))
                .thenReturn(Optional.ofNullable(messageType.isEmpty() ? null : messageType));
    }

    @When("a message with type {string} is received")
    public void aMessageWithTypeIsReceived(String messageType) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", messageType);
        payload.put("content", "Test content");

        testMessage = MessageBuilder.createMessage(payload, new MessageHeaders(new HashMap<>()));
        when(mockMessageTypeResolver.readMessageType(testMessage))
                .thenReturn(Optional.of(messageType));

        listener.accept(testMessage);
    }

    @When("a message with null type is received")
    public void aMessageWithNullTypeIsReceived() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("content", "Test content");
        testMessage = MessageBuilder.createMessage(payload, new MessageHeaders(new HashMap<>()));

        when(mockMessageTypeResolver.readMessageType(testMessage))
                .thenReturn(Optional.empty());

        try {
            listener.accept(testMessage);
        } catch (Exception e) {
            textWorld.setLastException(e);
        }
    }

    @When("another message consumer is registered for test message")
    public void anotherMessageConsumerIsRegisteredForMessage() {
        MessageConsumer<TestTypeAwareMessage> duplicateConsumer = new TestMessageConsumer(false);
        addMessageConsumer(duplicateConsumer);
    }

    @When("a message consumer with non-TypeAwareMessage payload class is registered")
    public void aMessageConsumerWithNonTypeAwareMessagePayloadClassIsRegistered() {
        MessageConsumer<NonTypeAwareMessage> nonTypeAwareConsumer = new NonTypeAwareMessageConsumer();
        addMessageConsumer(nonTypeAwareConsumer);
    }

    @When("a message is received")
    public void aMessageIsReceived() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("content", "Test content");
        testMessage = MessageBuilder.createMessage(payload, new MessageHeaders(new HashMap<>()));
        listener.accept(testMessage);
    }

    @Then("the message should be dispatched to the correct consumer")
    public void theMessageShouldBeDispatchedToTheCorrectConsumer() {
        assertThat(consumerCalled)
                .as("consumer called flag")
                .isTrue();
    }

    @Then("the message should be dispatched to the unknown type consumer")
    public void theMessageShouldBeDispatchedToTheUnknownTypeConsumer() {
        assertThat(unknownTypeConsumerCalled)
                .as("unknown type consumer called flag")
                .isTrue();
    }

    @Then("the message type resolver should be called to resolve the message type")
    public void theMessageTypeResolverShouldBeCalledToResolveTheMessageType() {
        verify(mockMessageTypeResolver)
                .readMessageType(testMessage);
    }

    public void addMessageConsumer(MessageConsumer<?> consumer) {
        messageConsumers.add(consumer);
    }

    /**
     * Test implementation of {@link TypeAwareMessage}.
     */
    @Getter
    private static class TestTypeAwareMessage implements TypeAwareMessage<TestMessageType> {

        private final TestMessageType type;

        @Setter
        private String content;

        public TestTypeAwareMessage() {
            type = new TestMessageType("test-type");
        }

    }

    /**
     * Test implementation of {@link MessageType}.
     */
    @Value
    private static class TestMessageType implements MessageType {

        String value;

        @Override
        public String getMessageBrokerValue() {
            return value;
        }

    }

    /**
     * Non-TypeAwareMessage class for testing error cases.
     */
    @Value
    private static class NonTypeAwareMessage {

        String content;

    }

    /**
     * Consumer for NonTypeAwareMessage.
     */
    private static class NonTypeAwareMessageConsumer implements MessageConsumer<NonTypeAwareMessage> {

        @Override
        public String getMessageListenerId() {
            return "testListener";
        }

        @Override
        public void accept(NonTypeAwareMessage payload, MessageHeaders headers) {
            // do nothing
        }

    }

    /**
     * Test implementation of {@link TypeBasedDispatchingMessageListener}.
     */
    private class TestTypeBasedDispatchingMessageListener extends TypeBasedDispatchingMessageListener {

        private final boolean noConsumers;

        public TestTypeBasedDispatchingMessageListener(MessageTypeResolver messageTypeResolver) {
            this(messageTypeResolver, false);
        }

        public TestTypeBasedDispatchingMessageListener(MessageTypeResolver messageTypeResolver, boolean noConsumers) {
            super("test-app", messageConsumers, messageTypeResolver, annotationValidator,
                    new ObjectMapper()
                            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                            // process Java 8 dates
                            .registerModule(new JavaTimeModule()));
            this.noConsumers = noConsumers;
        }

        @Override
        public String getId() {
            return "testListener";
        }

        @Override
        protected List<MessageConsumer> findLinkedMessageConsumers() {
            if (noConsumers) {
                return List.of();
            }

            return messageConsumers;
        }

    }

    /**
     * Test implementation of {@link MessageConsumer}.
     */
    private class TestMessageConsumer implements MessageConsumer<TestTypeAwareMessage> {

        private final boolean consumeUnknown;

        public TestMessageConsumer(boolean consumeUnknown) {
            this.consumeUnknown = consumeUnknown;
        }

        @Override
        public String getMessageListenerId() {
            return "testListener";
        }

        @Override
        public boolean consumeUnknownMessageTypes() {
            return consumeUnknown;
        }

        @Override
        public void accept(TestTypeAwareMessage payload, MessageHeaders headers) {
            if (consumeUnknown) {
                unknownTypeConsumerCalled = true;
            } else {
                consumerCalled = true;
            }
        }

    }
}
