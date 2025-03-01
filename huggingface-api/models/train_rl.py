from stable_baselines3 import PPO
from db.postgres import load_template_matrix
from models.rl_env import BalanceAdjustmentEnv
import asyncio

async def train_rl_model():
    """Train RL model to balance matrices."""
    template_matrix = await load_template_matrix()

    env = BalanceAdjustmentEnv(template_matrix)
    model = PPO("MlpPolicy", env, verbose=1)

    model.learn(total_timesteps=5000)
    model.save("balance_rl_model")

asyncio.run(train_rl_model())
