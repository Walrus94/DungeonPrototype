import logging
import asyncio
from confluent_kafka import Consumer
from confluent_kafka.admin import AdminClient, NewTopic
from config.settings import KAFKA_BOOTSTRAP_SERVER, KAFKA_BALANCE_MATRIX_TOPIC, KAFKA_TOPIC_ITEM_NAMING, KAFKA_GAME_RESULTS_TOPIC
from kafka.process_balance import process_kafka_balance_message
from kafka.process_item_metadata import process_kafka_item_message
from kafka.process_game_results import process_kafka_game_result

logging.basicConfig(level=logging.DEBUG)

def log_environment():
    """Log kafka related environment variables for troubleshooting"""
    logging.debug(
        "BOOTSTRAP: %s, TOPICS: %s, %s, %s",
        KAFKA_BOOTSTRAP_SERVER,
        KAFKA_BALANCE_MATRIX_TOPIC,
        KAFKA_TOPIC_ITEM_NAMING,
        KAFKA_GAME_RESULTS_TOPIC,
    )

def ensure_topics_exist():
    """Ensure required Kafka topics exist."""
    admin_client = AdminClient({'bootstrap.servers': KAFKA_BOOTSTRAP_SERVER})

    topics = [
        NewTopic(KAFKA_BALANCE_MATRIX_TOPIC, num_partitions=3, replication_factor=1),
        NewTopic(KAFKA_TOPIC_ITEM_NAMING, num_partitions=3, replication_factor=1),
        NewTopic(KAFKA_GAME_RESULTS_TOPIC, num_partitions=3, replication_factor=1),
    ]

    futures = admin_client.create_topics(topics)

    for topic, future in futures.items():
        try:
            future.result()  # Wait for topic creation
            logging.info(f"Topic '{topic}' created successfully.")
        except Exception as e:
            if "already exists" in str(e):
                logging.info(f"Topic '{topic}' already exists.")
            else:
                logging.error(f"Failed to create topic '{topic}': {e}")

# Kafka Consumer Configuration
def initialize_consumer() -> Consumer:
    ensure_topics_exist()  # Ensure topics are created before subscribing
    log_environment()  # Log environment variables for debugging
    kafka_consumer = Consumer({
        'bootstrap.servers': KAFKA_BOOTSTRAP_SERVER,
        'group.id': 'dungeon-group',
        'auto.offset.reset': 'earliest'
    })
    kafka_consumer.subscribe([KAFKA_BALANCE_MATRIX_TOPIC, KAFKA_TOPIC_ITEM_NAMING, KAFKA_GAME_RESULTS_TOPIC])
    logging.info(
        "Subscribed to topics: %s", 
        [KAFKA_BALANCE_MATRIX_TOPIC, KAFKA_TOPIC_ITEM_NAMING, KAFKA_GAME_RESULTS_TOPIC]
    )
    return kafka_consumer

kafka_consumer = initialize_consumer()

async def consume_messages():
    """Listens for balance matrix requests."""
    try:
        while True:
            msg = kafka_consumer.poll(1.0)
            if msg is None:
                continue
            if msg.error():
                logging.error(f"Kafka error: {msg.error()}")
                continue

            message_value = msg.value().decode('utf-8')

            logging.debug(f"Received message: {message_value}")
            topic = msg.topic()
            if topic == KAFKA_TOPIC_ITEM_NAMING:
                await process_kafka_item_message(message_value)
            elif topic == KAFKA_BALANCE_MATRIX_TOPIC:
                await process_kafka_balance_message(message_value)
            elif topic == KAFKA_GAME_RESULTS_TOPIC:  # New topic for game results
                await process_kafka_game_result(message_value)
            else:  # Default case
                logging.warning(f"Unhandled topic: {topic}")
    except Exception as e:
        logging.error(f"Consumer loop failed: {e}", exc_info=True)
    finally:
        kafka_consumer.close()
        logging.info("Kafka consumer closed.")

if __name__ == '__main__':
    asyncio.run(consume_messages())
