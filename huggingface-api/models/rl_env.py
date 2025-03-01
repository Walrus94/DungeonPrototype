import asyncpg
import numpy as np
from config.settings import POSTGRES_USER, POSTGRES_PASSWORD, POSTGRES_DB, POSTGRES_HOST

async def load_template_matrix():
    """Loads predefined template matrix from PostgreSQL."""
    conn = await asyncpg.connect(
        user=POSTGRES_USER, password=POSTGRES_PASSWORD,
        database=POSTGRES_DB, host=POSTGRES_HOST
    )

    row = await conn.fetchrow("SELECT data FROM balance_matrices WHERE chat_id = 'TEMPLATE' LIMIT 1")
    await conn.close()

    return np.array(row["data"]) if row else None

async def save_balance_matrix(chat_id, name, matrix):
    """Stores generated balance matrix in PostgreSQL."""
    conn = await asyncpg.connect(
        user=POSTGRES_USER, password=POSTGRES_PASSWORD,
        database=POSTGRES_DB, host=POSTGRES_HOST
    )

    await conn.execute(
        "INSERT INTO balance_matrices (chat_id, name, data) VALUES ($1, $2, $3)",
        chat_id, name, matrix.tolist()
    )

    await conn.close()
