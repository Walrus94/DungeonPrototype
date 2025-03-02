import gym
import numpy as np
from gym import spaces

class BalanceAdjustmentEnv(gym.Env):
    """Custom RL environment for tuning balance matrices."""

    def __init__(self, template_matrix):
        super(BalanceAdjustmentEnv, self).__init__()
        
        self.template_matrix = template_matrix  # Base matrix
        self.current_matrix = np.copy(template_matrix)  # Working matrix

        # Action space: Increase, Decrease, Keep
        self.action_space = spaces.Discrete(3)

        # Observation space: Matrix values
        self.observation_space = spaces.Box(
            low=0, high=100, shape=self.template_matrix.shape, dtype=np.float32
        )

    def step(self, action):
        """Adjust matrix values dynamically."""
        row, col = np.random.randint(0, self.template_matrix.shape[0]), np.random.randint(0, self.template_matrix.shape[1])
        
        if action == 0:
            self.current_matrix[row, col] += 1
        elif action == 1:
            self.current_matrix[row, col] -= 1

        # Reward: Closer to 50% win rate
        win_rate = np.mean(self.current_matrix) / 2.0
        reward = -abs(win_rate - 50)

        return self.current_matrix, reward, False, {}

    def reset(self):
        """Reset matrix to base template."""
        self.current_matrix = np.copy(self.template_matrix)
        return self.current_matrix

