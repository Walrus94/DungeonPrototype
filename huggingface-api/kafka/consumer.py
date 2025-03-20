import logging
from confluent_kafka import Consumer
from config.settings import KAFKA_BOOTSTRAP_SERVER, KAFKA_BALANCE_MATRIX_TOPIC, KAFKA_TOPIC_ITEM_NAMING
from kafka.process_balance import process_kafka_balance_message
from kafka.process_item_naming import process_kafka_item_message

logging.basicConfig(level=logging.DEBUG)

# Kafka Consumer Configuration
kafka_consumer = Consumer({
    'bootstrap.servers': KAFKA_BOOTSTRAP_SERVER,
    'group.id': 'balance-processing-group',
    'auto.offset.reset': 'earliest'
})

kafka_consumer.subscribe([KAFKA_BALANCE_MATRIX_TOPIC, KAFKA_TOPIC_ITEM_NAMING])

def consume_messages():
    """Listens for balance matrix requests."""
    while True:
        msg = kafka_consumer.poll(1.0)
        if msg is None:
            continue
        if msg.error():
            logging.error(f"Kafka error: {msg.error()}")
            continue

        logging.debug(f"Received message: {msg.value().decode('utf-8')}")
        topic = msg.topic()
        if topic == KAFKA_TOPIC_ITEM_NAMING:
            process_kafka_item_message(msg.value().decode('utf-8'))
        elif topic == KAFKA_BALANCE_MATRIX_TOPIC:
            process_kafka_balance_message(msg.value().decode('utf-8'))

if __name__ == '__main__':
    consume_messages()
