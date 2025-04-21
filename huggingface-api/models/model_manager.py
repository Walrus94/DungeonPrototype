from stable_baselines3 import PPO
import threading
from typing import Optional
import os
from config.settings import HF_MODEL_FILE
from datetime import datetime
from some_module import load_template_matrix, BalanceAdjustmentEnv

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
    
    def load_model(self):
        """Thread-safe model loading"""
        if self.model is None:
            with self.model_lock:
                if self.model is None:
                    model_path = os.path.join(HF_MODEL_FILE, "trained_models", "balance_rl_model")
                    if os.path.exists(f"{model_path}.zip"):
                        self.model = PPO.load(model_path)
                    
    def predict(self, observation, deterministic: bool = True):
        """Thread-safe prediction"""
        with self.model_lock:
            return self.model.predict(observation, deterministic=deterministic)

class AutoModelManager(ModelManager):
    async def train_on_new_data(self, game_result):
        """Incrementally train model on new game result"""
        async with self.model_lock:
            try:
                # Update environment with new game result
                template_matrix = await load_template_matrix("default_matrix")
                env = BalanceAdjustmentEnv(template_matrix, [game_result], "default_matrix")
                
                # Load or create model
                if self.model is None:
                    if os.path.exists(f"{self.model_path}.zip"):
                        self.model = PPO.load(self.model_path, env=env)
                    else:
                        self.model = PPO(
                            "MlpPolicy",
                            env,
                            learning_rate=5e-5,
                            n_steps=1024,  # Smaller steps for incremental training
                            batch_size=64,
                            n_epochs=4,
                            verbose=1
                        )

                # Perform quick update with new data
                self.model.learn(
                    total_timesteps=1000,  # Shorter training for incremental updates
                    reset_num_timesteps=False  # Continue from previous state
                )
                
                # Save updated model
                os.makedirs(os.path.dirname(self.model_path), exist_ok=True)
                self.model.save(self.model_path)
                
                self.last_training = datetime.now()
                
            except Exception as e:
                print(f"Error training on new data: {e}")
                raise