import logging
import json
from models.predict import generate_balance_matrix
from db.mongo import save_balance_matrix

async def process_kafka_balance_message(message):
    """Handles multiple balance matrix generation requests from Kafka."""
    logging.debug("Processing Kafka message for balance matrix generation.")
    try:
        data = json.loads(message)
        chat_id = data['chatId']
        matrix_requests = data['requests']  # List of matrix requests
        
        for request in matrix_requests:
            try:
                matrix_name = request['name']
                rows = request['rows']
                columns = request['cols']
                
                logging.debug(f"Generating balance matrix for chatId: {chat_id}, name: {matrix_name}")
                
                new_matrix = await generate_balance_matrix(
                    chat_id=chat_id,
                    matrix_name=matrix_name,
                    rows=rows,
                    columns=columns
                )
                
                await save_balance_matrix(chat_id, matrix_name, new_matrix)
                logging.debug(f"Balance matrix saved for chatId: {chat_id}, name: {matrix_name}")
                
            except KeyError as ke:
                logging.error(f"Missing required field in matrix request: {ke}")
                continue
            except Exception as e:
                logging.error(f"Error processing matrix {matrix_name} for chat {chat_id}: {e}")
                continue
                
    except Exception as e:
        logging.error(f"Error processing message: {e}")
        return
