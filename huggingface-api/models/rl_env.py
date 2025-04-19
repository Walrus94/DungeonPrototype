import gym
import numpy as np
from gym import spaces

class BalanceAdjustmentEnv(gym.Env):
    """Custom RL environment for tuning balance matrices."""

    def __init__(self, template_matrix, game_results, matrix_name):
        super(BalanceAdjustmentEnv, self).__init__()
        
        self.template_matrix = template_matrix  # Base matrix
        self.current_matrix = np.copy(template_matrix)  # Working matrix
        self.game_results = game_results  # Loaded game results
        self.matrix_name = matrix_name  # Name of the matrix being trained

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

        # Calculate reward based on game results
        reward = self._calculate_reward()

        return self.current_matrix, reward, False, {}

    def reset(self):
        """Reset matrix to base template."""
        self.current_matrix = np.copy(self.template_matrix)
        return self.current_matrix
    
    def _calculate_reward(self):
        """
        Calculate reward based on game results.
        """
        reward = 0
        if self.matrix_name in ["player_attack", "monster_attack"]:
            # Reward logic for balance matrices
            for game_result in self.game_results:
                # Penalize for death caused by a monster, scaled by the killer's weight
                if game_result["killer"]:
                    killer_weight_norm = np.linalg.norm(game_result["killer"]["weight"])
                    reward -= 10  # Base penalty for death
                    reward -= killer_weight_norm * 0.5  # Additional penalty scaled by killer's weight norm

                # Reward for maintaining stable player weight
                weight_variation = np.std(game_result["player_weight_dynamic"], axis=0)
                reward -= np.sum(weight_variation) * 0.1  # Penalize high weight variation

                # Reward for player level progression
                reward += len(game_result["player_level_progression"]) * 2  # Reward for reaching higher levels

                # Reward for dungeon level progression
                reward += len(game_result["dungeon_level_progression"]) * 1.5  # Reward for progressing through dungeon levels

                # Reward for defeating monsters, scaled by their weight
                for monster in game_result["defeated_monsters"]:
                    monster_weight = np.linalg.norm(monster["weight"])  # Sum of monster's weight vector
                    reward += monster_weight * 0.5  # Reward scaled by monster's weight
                    reward -= monster["battle_steps"] * 0.01  # Penalize for taking too many steps
        elif self.matrix_name.endswith("_attr") or self.matrix_name.endswith("_adjustment"):
            # Reward logic for item-related matrices
            for game_result in self.game_results:
                if "weightScale" in game_result:
                    weight_scale = game_result["weightScale"]
                    reward += self._calculate_weight_scale_reward(weight_scale)
                else:
                    reward -= 10  # Penalize if weightScale is missing

        return reward

    def _calculate_weight_scale_reward(self, weight_scale):
        """
        Calculate reward based on the weightScale dictionary.
        Args:
            weight_scale: Dictionary where keys are weight norms (float) and values are counts (int).
        Returns:
            Reward value based on the spread and distribution of weights.
        """
        reward = 0

        # Spread of weight norms (keys)
        weight_norms = np.array(list(weight_scale.keys()))
        if len(weight_norms) > 1:
            spread = np.max(weight_norms) - np.min(weight_norms)  # Range of weight norms
            reward += spread * 2  # Reward for wider spread of weights

        # Penalize clustering (high counts for specific weights)
        counts = np.array(list(weight_scale.values()))
        if len(counts) > 0:
            reward -= np.sum(counts ** 2) * 0.01  # Penalize high clustering (quadratic penalty)

        return reward
