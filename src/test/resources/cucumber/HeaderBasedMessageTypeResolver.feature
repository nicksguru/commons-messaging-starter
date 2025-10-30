@messaging #@disabled
Feature: HeaderBasedMessageTypeResolver
  As a messaging component
  The header based message type resolver should be able to read and write message types from/to headers
  So that messages can be properly routed and processed

  Scenario Outline: Reading message type from headers
    Given a message with header "<headerName>" set to "<headerValue>"
    When the message type is read from the message headers
    Then the message type should be "<expectedType>"
    Examples:
      | headerName   | headerValue | expectedType |
      | message-type | ORDER       | ORDER        |
      | message-type | PAYMENT     | PAYMENT      |
      | message-type |             |              |
      | wrong-header | ORDER       |              |

  Scenario Outline: Writing message type from TypeAwareMessage to target headers
    Given a type aware message with type "<messageType>"
    And empty target payload and headers
    When the message type is written to the target
    Then the target header "<headerName>" should be "<expectedValue>"
    Examples:
      | messageType | headerName   | expectedValue |
      | ORDER       | message-type | ORDER         |
      | PAYMENT     | message-type | PAYMENT       |
      |             | message-type |               |

  Scenario Outline: Writing message type from source payload to target headers
    Given a source payload with field "<fieldName>" set to "<fieldValue>"
    And empty target payload and headers
    When the message type is written from source payload to target
    Then the target header "<headerName>" should be "<expectedValue>"
    Examples:
      | fieldName   | fieldValue | headerName   | expectedValue |
      | messageType | ORDER      | message-type | ORDER         |
      | messageType | PAYMENT    | message-type | PAYMENT       |
      | messageType |            | message-type |               |
      | wrongField  | ORDER      | message-type |               |
