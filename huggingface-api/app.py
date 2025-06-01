import logging
import asyncio
from kafka.consumer import consume_messages
from db.mongo import init_balance_matrices_index

logging.basicConfig(level=logging.DEBUG)

def init_app():
    """Initialize application dependencies"""
    logging.info("Initializing application...")
    try:
        init_balance_matrices_index()
        logging.info("Application initialization completed")
    except Exception as e:
        logging.error(f"Failed to initialize application: {e}")
        raise

if __name__ == '__main__':
    init_app()
    logging.info("Starting Kafka consumer")
    asyncio.run(consume_messages())
