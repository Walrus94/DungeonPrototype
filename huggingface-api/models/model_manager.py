from stable_baselines3 import PPO
import threading
from typing import Optional
import os
import logging
from config.settings import HF_MODEL_FILE
from datetime import datetime
from db.postgres import load_template_matrix, load_chat_matrices, load_balance_matrix
from models.rl_env import BalanceAdjustmentEnv

class ModelManager:
    _instance = None
    _lock = threading.Lock()
    
    def __init__(self):
        self.model: Optional[PPO] = None
        self.model_lock = threading.Lock()
        
    @classmethod
    def get_instance(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = ModelManager()
        return cls._instance
    
    def get_or_create_model(self, env):
        """Thread-safe model loading or creation"""
        with self.model_lock:
            model_path = os.path.join(HF_MODEL_FILE, "trained_models", "balance_rl_model")
            if os.path.exists(f"{model_path}.zip"):
                self.model = PPO.load(model_path, env=env)
            else:
                self.model = self.initialize_model(env)
            return self.model

    def predict(self, observation, deterministic: bool = True):
        """Thread-safe prediction"""
        with self.model_lock:
            return self.model.predict(observation, deterministic=deterministic)

    def initialize_model(self, env):
        """Initialize a new model with given environment"""
        self.model = PPO(
            "MlpPolicy",
            env,
            learning_rate=5e-5,  # Reduced learning rate for finer adjustments
            n_steps=2048,        # Increased steps per update
            batch_size=64,
            n_epochs=10,         # More epochs per update
            gamma=0.99,          # High discount factor for long-term effects
            ent_coef=0.01,      # Encourage exploration
            policy_kwargs=dict(
                net_arch=dict(
                    pi=[128, 128],  # Policy network
                    vf=[128, 128]   # Value network
                ),
            ),
            device='cpu'      # Force CPU usage to save GPU memory
        )
        return self.model

class AutoModelManager(ModelManager):
    async def train_on_new_data(self, game_result):
        """Incrementally train model on new game result for all matrices associated with the chat."""
        async with self.model_lock:
            try:
                chat_id = game_result.get('chat_id')
                if not chat_id:
                    logging.error("No chat_id in game result")
                    return

                # Load all matrices associated with this chat
                matrices = await load_chat_matrices(chat_id)
                if not matrices:
                    logging.warning(f"No matrices found for chat {chat_id}")
                    return

                for name, data in matrices.items():
                    logging.info(f"Training model for matrix: {name}")
                    
                    # Create environment for this matrix
                    env = BalanceAdjustmentEnv(data, [game_result], name)
                    
                    # Use get_or_create_model instead of direct loading/creation
                    self.model = self.get_or_create_model(env)
                    
                    # Perform quick update with new data
                    self.model.learn(
                        total_timesteps=1000,
                        reset_num_timesteps=False
                    )
                    
                    # Save updated model
                    model_path = os.path.join(HF_MODEL_FILE, "trained_models", "balance_rl_model")
                    os.makedirs(os.path.dirname(model_path), exist_ok=True)
                    self.model.save(model_path)
                    
                    logging.info(f"Completed training for matrix: {name}")
                
                self.last_training = datetime.now()
                    
            except Exception as e:
                logging.error(f"Error training on new data: {e}")
                raise

    def save_model(self):
        """Thread-safe model saving"""
        with self.model_lock:
            if self.model is not None:
                try:
                    os.makedirs(os.path.dirname(self.model_path), exist_ok=True)
                    self.model.save(self.model_path)
                    return True
                except Exception as e:
                    logging.error(f"Error saving model: {e}")
                    return False
            return False

    def fine_tune_and_save(self, timesteps=2000):
        """Thread-safe fine-tuning and saving"""
        with self.model_lock:
            if self.model is not None:
                try:
                    self.model.learn(
                        total_timesteps=timesteps,
                        progress_bar=True
                    )
                    return self.save_model()
                except Exception as e:
                    logging.error(f"Error fine-tuning model: {e}")
                    return False
            return False