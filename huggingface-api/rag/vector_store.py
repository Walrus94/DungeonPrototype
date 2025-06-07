import os
import json
from typing import List
from config.settings import RAG_STORE_PATH


def _ensure_dir():
    os.makedirs(RAG_STORE_PATH, exist_ok=True)


def _get_store_path(chat_id: int) -> str:
    _ensure_dir()
    return os.path.join(RAG_STORE_PATH, f"{chat_id}_weights.json")


def add_weight_vector(chat_id: int, vector: List[float]) -> None:
    """Append a weight vector to the chat's vector store."""
    path = _get_store_path(chat_id)
    try:
        if os.path.exists(path):
            with open(path, "r") as f:
                data = json.load(f)
        else:
            data = []
    except Exception:
        data = []
    data.append(vector)
    with open(path, "w") as f:
        json.dump(data, f)


def get_weight_vectors(chat_id: int, limit: int = 50) -> List[List[float]]:
    """Retrieve up to `limit` weight vectors for the chat."""
    path = _get_store_path(chat_id)
    if not os.path.exists(path):
        return []
    try:
        with open(path, "r") as f:
            data = json.load(f)
    except Exception:
        return []
    if limit:
        return data[-limit:]
    return data


def update_store_with_game_result(game_result: dict) -> None:
    """Extract final player weight from game result and store it."""
    chat_id = game_result.get("chat_id")
    weight_dynamic = game_result.get("playerWeightDynamic")
    if chat_id is None or not weight_dynamic:
        return
    final_vector = weight_dynamic[-1]
    if isinstance(final_vector, list) and all(isinstance(v, (int, float)) for v in final_vector):
        add_weight_vector(chat_id, final_vector)

