from huggingface_sb3 import load_from_hub
from stable_baselines3 import PPO
from models.rl_env import BalanceAdjustmentEnv
from db.postgres import load_template_matrix
from db.mongo import load_game_results
from models.model_manager import ModelManager
import numpy as np
import logging

async def generate_balance_matrix(chat_id, matrix_name, columns, rows):
    """Generate a balanced matrix based on game results and template matrix."""
    # Load the template matrix and game results
    template_matrix = await load_template_matrix(matrix_name)
    game_results = await load_game_results(chat_id)

    # Generate a new matrix of the specified size
    generated_matrix = np.zeros((rows, columns))
    for i in range(rows):
        for j in range(columns):
            if template_matrix is not None and i < len(template_matrix) and j < len(template_matrix[i]) and template_matrix[i][j] != 0.0:
                # Use value from the template matrix if present
                generated_matrix[i][j] = template_matrix[i][j]
            else:
                # Generate a value between 0.7 and 1.3
                generated_matrix[i][j] = np.random.uniform(0.7, 1.3)

    # Adjust the matrix using the trained RL model
    try:
        # Create environment
        env = BalanceAdjustmentEnv(generated_matrix, game_results, matrix_name)

        obs = env.reset()

        # Get model manager instance and get/create model
        model_manager = ModelManager.get_instance()
        model = model_manager.get_or_create_model(env)

        # Fine-tune model
        model_manager.fine_tune_and_save(timesteps=2000)
        
        
        # More adjustment steps for larger matrices
        num_steps = max(10, int(np.sqrt(rows * columns)))
        for _ in range(num_steps):
            action, _ = model.predict(obs, deterministic=True)
            obs, _, done, _ = env.step(action)
            if done:
                break

        return obs
    except Exception as e:
        print(f"Error applying model adjustments: {e}")
        return generated_matrix
