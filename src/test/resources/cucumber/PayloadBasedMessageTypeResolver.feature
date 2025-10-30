@messaging #@disabled
Feature: PayloadBasedMessageTypeResolver
  As a messaging component
  The payload based message type resolver should properly read and write message types
  So that messages can be properly routed and processed

  Background:
    Given a PayloadBasedMessageTypeResolver is initialized with field name "messageType"

  Scenario Outline: Reading message type from payload
    Given a message with payload containing "<fieldName>" with value "<fieldValue>"
    When the message type is read from the message payload
    Then the result should contain "<expectedResult>"
    Examples:
      | fieldName   | fieldValue    | expectedResult |
      | messageType | ORDER_CREATED | ORDER_CREATED  |
      | messageType |               |                |
      | wrongField  | ORDER_CREATED |                |
      | messageType | null          |                |

  Scenario: Reading message type from empty payload
    Given a message with empty payload
    When the message type is read from the message payload
    Then the result should be empty

  Scenario Outline: Writing message type from TypeAwareMessage to payload
    Given a TypeAwareMessage with type "<messageType>"
    And an empty target payload
    And an empty target headers map
    When the message type is written from TypeAwareMessage to payload
    Then the target payload should contain key "messageType" with value "<expectedValue>"
    Examples:
      | messageType   | expectedValue |
      | ORDER_CREATED | ORDER_CREATED |
      |               |               |
      | null          |               |

  Scenario Outline: Writing message type from source payload to target payload
    Given a source payload with "messageType" set to "<sourceValue>"
    And an empty target payload
    And an empty target headers map
    When the message type is written from source payload to target payload
    Then the target payload should contain key "messageType" with value "<expectedValue>"
    Examples:
      | sourceValue   | expectedValue |
      | ORDER_CREATED | ORDER_CREATED |
      |               |               |
      | null          |               |

  Scenario: Initialization with blank field name
    When the PayloadBasedMessageTypeResolver is initialized with blank payload message type field
    Then the exception message should contain "must not be blank"
