from stable_baselines3 import PPO
from models.rl_env import BalanceAdjustmentEnv
from db.postgres import load_template_matrix
from db.mongo import load_game_results
import numpy as np

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
    # Load the template matrix from the database
    template_matrix = await load_template_matrix(chat_id, matrix_name)

    # Load game results from MongoDB
    game_results = await load_game_results(chat_id)

    # Train the RL model using game results
    env = BalanceAdjustmentEnv(template_matrix, game_results)
    model = PPO("MlpPolicy", env, verbose=1)
    model.learn(total_timesteps=10000)  # Adjust timesteps as needed

    # Generate a new matrix of the specified size
    generated_matrix = np.zeros((rows, columns))
    for i in range(rows):
        for j in range(columns):
            if template_matrix != None and i < len(template_matrix) and j < len(template_matrix[i]) and template_matrix[i][j] != 0.0:
                # Use value from the template matrix if present
                generated_matrix[i][j] = template_matrix[i][j]
            else:
                # Generate a value between 0.7 and 1.3
                generated_matrix[i][j] = np.random.uniform(0.7, 1.3)

    # Adjust the matrix using the trained RL model
    """Use trained RL model to generate a balanced matrix."""
    model = PPO.load("balance_rl_model")

    env = BalanceAdjustmentEnv(generated_matrix, game_results, matrix_name)
    obs = env.reset()

    for _ in range(10):  # Adjust matrix over 10 steps
        action, _ = model.predict(obs)
        obs, _, _, _ = env.step(action)

    return obs
