@messaging #@disabled
Feature: Type Based Dispatching Message Listener
  Tests for the TypeBasedDispatchingMessageListener implementation

  Scenario: Message is dispatched to the correct consumer based on message type
    Given a message consumer is registered for test message
    And a type based dispatching message listener is initialized
    And no exception should be thrown
    When a message with type "test-type" is received
    Then no exception should be thrown
    And the message should be dispatched to the correct consumer

  Scenario: Message with unknown type is dispatched to unknown type consumer
    Given  a message consumer is registered for unknown message types
    And a type based dispatching message listener is initialized
    And no exception should be thrown
    When a message with type "unknown-type" is received
    Then no exception should be thrown
    And the message should be dispatched to the unknown type consumer

  Scenario: Message with null type is dispatched to unknown type consumer
    Given a message consumer is registered for unknown message types
    And a type based dispatching message listener is initialized
    And no exception should be thrown
    When a message with null type is received
    Then no exception should be thrown
    And the message should be dispatched to the unknown type consumer

  Scenario: Multiple consumers for the same message type causes exception
    Given a message consumer is registered for test message
    And another message consumer is registered for test message
    When a type based dispatching message listener is initialized
    Then the exception message should contain "message type 'test-type' is already bound"

  Scenario: Non-TypeAwareMessage payload class causes exception
    Given a message consumer with non-TypeAwareMessage payload class is registered
    When a type based dispatching message listener is initialized
    Then the exception message should contain "Payload class must inherit from"

  Scenario Outline: Message type resolution is delegated to message type resolver
    Given a type based dispatching message listener is initialized
    And no exception should be thrown
    And a message type resolver that resolves message type to "<resolved_type>"
    When a message is received
    Then the message type resolver should be called to resolve the message type
    Examples:
      | resolved_type |
      | test-type-1   |
      | test-type-2   |
      | unknown-type  |
      |               |
