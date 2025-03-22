from stable_baselines3 import PPO
from models.rl_env import BalanceAdjustmentEnv
from db.postgres import load_template_matrix

async def generate_balance_matrix(chat_id, database, columns, rows):
    """TODO: Adjust matrix size based on columns and rows."""
    """Use trained RL model to generate a balanced matrix."""
    template_matrix = await load_template_matrix(chat_id, database)
    model = PPO.load("balance_rl_model")

    env = BalanceAdjustmentEnv(template_matrix)
    obs = env.reset()

    for _ in range(10):  # Adjust matrix over 10 steps
        action, _ = model.predict(obs)
        obs, _, _, _ = env.step(action)

    return obs
