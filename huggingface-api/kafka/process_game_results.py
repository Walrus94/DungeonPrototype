import json
import logging
from models.model_manager import AutoModelManager
from db.mongo import save_game_result
from typing import Dict, Any

async def process_kafka_game_result(message: str) -> None:
    """
    Process game results from Kafka:
    1. Parse game result data
    2. Save to MongoDB
    3. Update RL model with new data
    
    Args:
        message (str): JSON string containing game result data
    """
    try:
        # Parse message
        game_result: Dict[str, Any] = json.loads(message)
        
        # Save to MongoDB
        await save_game_result(game_result)
        logging.info(f"Game result saved to MongoDB for chat_id: {game_result.get('chat_id')}")

        # Get model manager instance
        model_manager = AutoModelManager.get_instance()
        
        # Train model with new data
        await model_manager.train_on_new_data(game_result)
        logging.info("Model training completed with new game result")

    except json.JSONDecodeError as e:
        logging.error(f"Failed to parse game result JSON: {e}")
        raise
    except Exception as e:
        logging.error(f"Error processing game result: {e}")
        raise