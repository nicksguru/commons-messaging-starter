@messaging #@disabled
Feature: Kafka Message Publisher Service
  The Kafka message publisher service should be able to publish messages to Kafka topics

  Scenario Outline: Publishing messages with different payload types
    Given a message with topic "<topic>" and payload type "<payloadType>"
    And the payload has message type "<messageType>"
    When the message is published
    Then the message should be sent to the topic
    Examples:
      | topic         | payloadType      | messageType  |
      | orders        | TypeAwareMessage | ORDER        |
      | payments      | TypeAwareMessage | PAYMENT      |
      | notifications | Map              | NOTIFICATION |
      | events        | TypeAwareMessage | EVENT        |

  Scenario Outline: Publishing messages with message keys
    Given a message with topic "<topic>" and payload type "<payloadType>"
    And the payload has message type "<messageType>"
    And the message key is "<messageKey>"
    When the message is published with string key
    Then the message should be sent to the topic
    And the message headers should contain the message key
    Examples:
      | topic         | payloadType      | messageType  | messageKey  |
      | orders        | TypeAwareMessage | ORDER        | order-key-1 |
      | payments      | TypeAwareMessage | PAYMENT      | payment-key |
      | events        | Map              | EVENT        | event-key   |
      | notifications | TypeAwareMessage | NOTIFICATION | notify-key  |

  Scenario Outline: Publishing messages with byte array message keys
    Given a message with topic "<topic>" and payload type "<payloadType>"
    And the payload has message type "<messageType>"
    And the message key as bytes is "<messageKey>"
    When the message is published with byte key
    Then the message should be sent to the topic
    And the message headers should contain the message key as bytes
    Examples:
      | topic    | payloadType      | messageType | messageKey  |
      | orders   | TypeAwareMessage | ORDER       | order-key-1 |
      | payments | Map              | PAYMENT     | payment-key |
      | events   | TypeAwareMessage | EVENT       |             |

  Scenario: Publishing message with unsupported payload type
    Given a message with topic "orders" and payload type "String"
    And the payload has message type "ORDER"
    When the message is published
    Then an exception should be thrown
