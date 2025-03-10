import logging
import json
from models.predict import generate_balance_matrix
from db.postgres import save_balance_matrix

async def process_kafka_balance_message(message):
    """Handles balance matrix generation requests from Kafka."""
    try:
        data = json.loads(message)
        chat_id = data['chatId']
        database = data['database']
        matrix_name = data['name']
    except Exception as e:
        logging.error(f"Error processing message: {str(e)}")
        return

    logging.debug(f"Generating balance matrix for chatId: {chat_id}, name: {matrix_name}")

    new_matrix = await generate_balance_matrix(chat_id, database)
    await save_balance_matrix(chat_id, matrix_name, new_matrix)

    logging.debug(f"Balance matrix saved for chatId: {chat_id}")
