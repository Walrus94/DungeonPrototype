import logging
from config.settings import MONGO_DATABASE_NAME, MONGO_DATABASE_PASSWORD, MONGO_DATABASE_PORT, MONGO_DATABASE_USER
from pymongo import MongoClient
from bson.objectid import ObjectId
from typing import Dict, Any

mongo_client = MongoClient("mongodb://" + MONGO_DATABASE_USER + ":" + MONGO_DATABASE_PASSWORD +
                           "@" +
                           "mongo:" + MONGO_DATABASE_PORT +
                           "/?authMechanism=SCRAM-SHA-1")
db = mongo_client[MONGO_DATABASE_NAME]
items_collection = db['items']
game_results_collection = db['game_results']

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
        results = game_results_collection.find({"chat_id": chat_id})
        return list(results)
    except Exception as e:
        logging.error(f"Error loading game results from MongoDB: {str(e)}")
        return []

async def save_game_result(game_result: Dict[str, Any]) -> None:
    """Save game result to MongoDB"""
    await game_results_collection.insert_one(game_result)