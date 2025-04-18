import logging
import asyncio
from kafka.consumer import consume_messages

logging.basicConfig(level=logging.DEBUG)


if __name__ == '__main__':
    logging.info("Starting Kafka consumer")
    asyncio.run(consume_messages())
