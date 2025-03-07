import logging
from kafka.consumer import consume_messages

logging.basicConfig(level=logging.DEBUG)


if __name__ == '__main__':
    logging.info("Starting Kafka consumer")
    consume_messages()
