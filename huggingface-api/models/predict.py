from huggingface_sb3 import load_from_hub
from stable_baselines3 import PPO
from models.rl_env import BalanceAdjustmentEnv
from db.postgres import load_template_matrix
from db.mongo import load_game_results
from config.settings import HF_MODEL_FILE
import numpy as np
import os
import logging

async def generate_balance_matrix(chat_id, matrix_name, columns, rows):
    """
    Generate a balanced matrix based on game results and template matrix.
    Args:
        chat_id: ID of the chat.
        database: Database connection.
        columns: Number of columns in the matrix.
        rows: Number of rows in the matrix.
        game_results: List of game results, including reached level, statistics, and player state dynamics.
    Returns:
        Generated matrix of the specified size.
    """
    # Directory to save trained models
    trained_models_dir = os.path.join(HF_MODEL_FILE, "trained_models")
    # Load the template matrix from the database
    template_matrix = await load_template_matrix(matrix_name)
    # Load game results from MongoDB
    game_results = await load_game_results(chat_id)

    # Create environment
    env = BalanceAdjustmentEnv(template_matrix, game_results, matrix_name)


    model = PPO.load(
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

    # Quick fine-tuning
    model.learn(
        total_timesteps=2000,
        progress_bar=True
    )

    # Save the fine-tuned model
    
    os.makedirs(trained_models_dir, exist_ok=True)
    model.save(os.path.join(trained_models_dir, "balance_rl_model"))


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
     # Apply trained model adjustments
    try:
        env = BalanceAdjustmentEnv(generated_matrix, game_results, matrix_name)
        obs = env.reset()
        
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
