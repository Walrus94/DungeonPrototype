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
            low=0, high=2, shape=self.template_matrix.shape, dtype=np.float32
        )

    def step(self, action):
        """Adjust matrix values dynamically."""
        row, col = np.random.randint(0, self.template_matrix.shape[0]), np.random.randint(0, self.template_matrix.shape[1])
        
        change_amount = 0.01
        done = False

        # Apply changes with bounds checking
        if action == 0 and self.current_matrix[row, col] < 2.0:
            self.current_matrix[row, col] = np.round(self.current_matrix[row, col] + change_amount, 4)
            self.total_changes[row, col] += 1
        elif action == 1 and self.current_matrix[row, col] > 0.1:
            self.current_matrix[row, col] = np.round(self.current_matrix[row, col] - change_amount, 4)
            self.total_changes[row, col] += 1

        # Update episode tracking
        self.current_changes += 1
        if self.current_changes >= self.max_changes:
            done = True

        # Calculate reward based on game results
        reward = self._calculate_reward()

        # Add stability penalty
        stability_penalty = np.sum(self.total_changes) * 0.001  # Penalize too many changes
        reward -= stability_penalty

        info = {
            'changes': self.total_changes,
            'current_value': self.current_matrix[row, col],
            'position': (row, col)
        }

        return self.current_matrix, reward, done, info

    def reset(self):
        """Reset matrix to base template."""
        self.current_matrix = np.copy(self.template_matrix)
        self.total_changes = np.zeros_like(self.template_matrix)
        self.current_changes = 0
        return self.current_matrix
    
    def _calculate_reward(self):
        """Calculate reward based on game results considering weight vectors."""
        reward = 0
        if self.matrix_name in ["player_attack", "monster_attack"]:
            for game_result in self.game_results:
                # Base survival reward
                if not game_result["death"]:
                    reward += 20
                
                # Analyze weight vector progression
                if game_result["playerWeightDynamic"]:
                    weight_vectors = np.array([w for w in game_result["playerWeightDynamic"]])
                    
                    # Calculate weight vector stability per component
                    if len(weight_vectors) > 1:
                        # Analyze each weight component separately
                        component_stability = []
                        for component_idx in range(weight_vectors.shape[1]):
                            component_trajectory = weight_vectors[:, component_idx]
                            # Reward steady growth in each component
                            component_growth = np.diff(component_trajectory)
                            stability = 1 / (np.std(component_growth) + 1)
                            component_stability.append(stability)
                        
                        # Average stability across all weight components
                        reward += np.mean(component_stability) * 15
                
                # Combat effectiveness with vector weights
                if game_result["defeatedMonsters"]:
                    for monster in game_result["defeatedMonsters"]:
                        player_weight = game_result["playerWeightDynamic"][monster["stepKilled"]]
                        monster_weight = monster["weight"]
                        
                        # Calculate weight advantage/disadvantage
                        weight_diff = np.array(player_weight) - np.array(monster_weight)
                        relative_strength = np.linalg.norm(weight_diff)
                        
                        if relative_strength < 0:
                            # Reward for defeating stronger monsters
                            reward += abs(relative_strength) * 3
                        else:
                            # Small reward for defeating weaker monsters efficiently
                            reward += 1 / (monster["battleSteps"] + 1)
                
                # Death analysis with vector comparison
                if game_result["death"] and game_result["killer"]:
                    killer_weight = np.array(game_result["killer"]["weight"])
                    final_player_weight = np.array(game_result["playerWeightDynamic"][-1])
                    
                    # Compare weight vectors using cosine similarity
                    cos_sim = np.dot(killer_weight, final_player_weight) / \
                             (np.linalg.norm(killer_weight) * np.linalg.norm(final_player_weight))
                    
                    # Calculate vector magnitude difference
                    magnitude_diff = np.linalg.norm(killer_weight) - np.linalg.norm(final_player_weight)
                    
                    if magnitude_diff > 0:
                        # Less penalty if died to a significantly stronger monster
                        relative_penalty = 1 - min(1, magnitude_diff / np.linalg.norm(final_player_weight))
                        reward -= 10 * relative_penalty * (0.5 + 0.5 * cos_sim)
                    else:
                        # Severe penalty for dying to weaker monsters
                        reward -= 25 * (1 + cos_sim)

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
