import logging
import numpy as np
from config.settings import MONGO_DATABASE_NAME, MONGO_DATABASE_PASSWORD, MONGO_DATABASE_PORT, MONGO_DATABASE_USER
from pymongo import MongoClient
from bson.objectid import ObjectId
from typing import Dict, Any, Optional

mongo_client = MongoClient("mongodb://" + MONGO_DATABASE_USER + ":" + MONGO_DATABASE_PASSWORD +
                           "@" +
                           "mongo:" + MONGO_DATABASE_PORT +
                           "/?authMechanism=SCRAM-SHA-1")
db = mongo_client[MONGO_DATABASE_NAME]
items_collection = db['items']
game_results_collection = db['game_results']
template_matrices_collection = db['template_matrices']
balance_matrices_collection = db['balance_matrices']

def update_mongo_item(chat_id, item_id, generated_name):
    try:
        logging.debug(f"Updating MongoDB item with chatId: {chat_id}, id: {item_id}, name: {generated_name}")
        
        # Find the document by chatId and id, and update the 'name' field
        result = items_collection.update_one(
            {'chatId': chat_id, '_id': ObjectId(item_id)},  # Query to find the document
            {'$set': {'name': generated_name}}   # Update operation to set the 'name' field
        )

        logging.debug(result)
        
        if result.matched_count > 0:
            logging.debug("MongoDB item updated successfully")
        else:
            logging.warning(f"No MongoDB document updated for chatId: {chat_id}, id: {item_id}")
    except Exception as e:
        logging.error(f"Error updating MongoDB: {str(e)}")

async def load_game_results(chat_id):
    """
    Load game results for a specific chat ID from MongoDB.
    Args:
        chat_id: ID of the chat.
    Returns:
        List of game results as dictionaries.
    """
    try:
        logging.debug(f"Loading game results for chatId: {chat_id}")
        results = game_results_collection.find({"chat_id": chat_id})
        return list(results)
    except Exception as e:
        logging.error(f"Error loading game results from MongoDB: {str(e)}")
        return []

async def save_game_result(game_result: Dict[str, Any]) -> None:
    """Save game result to MongoDB"""
    await game_results_collection.insert_one(game_result)

async def load_template_matrix(matrix_name: str) -> Optional[np.ndarray]:
    """Loads predefined template matrix from MongoDB."""
    try:
        logging.debug(f"Loading template matrix: {matrix_name}")
        doc = template_matrices_collection.find_one({'name': matrix_name})
        return np.array(doc['data'], dtype=np.float32) if doc else None
    except Exception as e:
        logging.error(f"Error loading template matrix: {e}")
        return None

async def save_balance_matrix(chat_id: int, name: str, matrix: np.ndarray) -> bool:
    """Stores or updates balance matrix in MongoDB."""
    try:
        logging.debug(f"Saving balance matrix for chatId: {chat_id}, name: {name}")
        result = balance_matrices_collection.update_one(
            {'chat_id': chat_id, 'name': name},
            {
                '$set': {
                    'data': matrix.tolist(),
                }
            },
            upsert=True
        )
        logging.debug(f"Successfully saved/updated matrix {name} for chat {chat_id}")
        return True
    except Exception as e:
        logging.error(f"Error saving matrix: {e}")
        return False

async def load_balance_matrix(chat_id: int, name: str) -> Optional[np.ndarray]:
    """Loads balance matrix from MongoDB."""
    try:
        logging.debug(f"Loading balance matrix for chatId: {chat_id}, name: {name}")
        doc = balance_matrices_collection.find_one({'chat_id': chat_id, 'name': name})
        return np.array(doc['data'], dtype=np.float32) if doc else None
    except Exception as e:
        logging.error(f"Error loading balance matrix: {e}")
        return None

async def load_chat_matrices(chat_id: int) -> Dict[str, np.ndarray]:
    """Load all balance matrices associated with a chat."""
    try:
        logging.debug(f"Loading all matrices for chatId: {chat_id}")
        cursor = balance_matrices_collection.find({'chat_id': chat_id})
        return {doc['name']: np.array(doc['data'], dtype=np.float32) 
                for doc in cursor}
    except Exception as e:
        logging.error(f"Error loading chat matrices: {e}")
        return {}