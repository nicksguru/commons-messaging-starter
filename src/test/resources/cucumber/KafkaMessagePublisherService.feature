@messaging #@disabled
Feature: Kafka Message Publisher Service
  The Kafka message publisher service should be able to publish messages to Kafka topics

  Scenario Outline: Publishing messages with different payload types
    Given a message with topic "<topic>" and payload type "<payloadType>"
    And the payload has message type "<messageType>"
    And the partition key is "<partitionKey>"
    When the message is published
    Then the message should be sent to the topic
    And the message headers should contain the partition key
    And the message type should be written to the payload
    Examples:
      | topic         | payloadType      | messageType  | partitionKey |
      | orders        | TypeAwareMessage | ORDER        | customer-123 |
      | payments      | TypeAwareMessage | PAYMENT      | customer-456 |
      | notifications | Map              | NOTIFICATION | customer-789 |
      | events        | TypeAwareMessage | EVENT        |              |

  Scenario Outline: Publishing messages with message keys
    Given a message with topic "<topic>" and payload type "<payloadType>"
    And the payload has message type "<messageType>"
    And the partition key is "<partitionKey>"
    And the message key is "<messageKey>"
    When the message is published with string key
    Then the message should be sent to the topic
    And the message headers should contain the partition key
    And the message headers should contain the message key
    And the message type should be written to the payload
    Examples:
      | topic         | payloadType      | messageType  | partitionKey | messageKey  |
      | orders        | TypeAwareMessage | ORDER        | customer-123 | order-key-1 |
      | payments      | TypeAwareMessage | PAYMENT      | customer-456 | payment-key |
      | events        | Map              | EVENT        | customer-789 | event-key   |
      | notifications | TypeAwareMessage | NOTIFICATION |              | notify-key  |

  Scenario Outline: Publishing messages with byte array message keys
    Given a message with topic "<topic>" and payload type "<payloadType>"
    And the payload has message type "<messageType>"
    And the partition key is "<partitionKey>"
    And the message key as bytes is "<messageKey>"
    When the message is published with byte key
    Then the message should be sent to the topic
    And the message headers should contain the partition key
    And the message headers should contain the message key as bytes
    And the message type should be written to the payload
    Examples:
      | topic    | payloadType      | messageType | partitionKey | messageKey  |
      | orders   | TypeAwareMessage | ORDER       | customer-123 | order-key-1 |
      | payments | Map              | PAYMENT     | customer-456 | payment-key |
      | events   | TypeAwareMessage | EVENT       | customer-789 |             |

  Scenario: Publishing message with unsupported payload type
    Given a message with topic "orders" and payload type "String"
    And the payload has message type "ORDER"
    And the partition key is "customer-123"
    When the message is published
    Then an exception should be thrown
