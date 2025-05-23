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
        self.current_changes = 0  # Initial changes counter
        self.max_changes = 100  # Max changes per episode
        self.total_changes = np.zeros_like(template_matrix)  # Track changes

        # Action space: continuous values for each matrix element
        self.action_space = spaces.Box(
            low=-1.0,
            high=1.0,
            shape=template_matrix.shape,
            dtype=np.float32
        )

        # Observation space: matrix of the same shape as template
        self.observation_space = spaces.Box(
            low=0.7,  # Minimum allowed value
            high=1.3,  # Maximum allowed value
            shape=template_matrix.shape,
            dtype=np.float32
        )
    
    def seed(self, seed=None):
        """Set random seed for reproducibility."""
        self.np_random, seed = gym.utils.seeding.np_random(seed)
        np.random.seed(seed)
        return [seed]

    def step(self, action):
        """Adjust matrix values dynamically."""
        done = False
        
        # Apply changes across the entire matrix
        change_amount = 0.01
        changes = np.clip(action * change_amount, -change_amount, change_amount)
        
        # Update matrix with bounds checking
        new_matrix = self.current_matrix + changes
        new_matrix = np.clip(new_matrix, 0.7, 1.3)  # Enforce bounds
        
        # Track changes
        actual_changes = new_matrix - self.current_matrix
        self.total_changes += np.abs(actual_changes) > 0
        self.current_matrix = new_matrix

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
            'matrix': self.current_matrix,
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
        else:
            # Handle other matrices using weight scale analysis
            for game_result in self.game_results:
                if "weightScale" in game_result:
                    weight_scale_reward = self._calculate_weight_scale_reward(game_result["weightScale"])
                    reward += weight_scale_reward

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
