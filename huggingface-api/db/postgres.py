import asyncpg
import logging
import numpy as np
from config.settings import POSTGRES_USER, POSTGRES_PASSWORD, POSTGRES_DB, POSTGRES_PORT

async def load_template_matrix(matrix_name):
    """Loads predefined template matrix from PostgreSQL."""
    logging.debug(f"Loading template matrix: {matrix_name}")
    conn = await asyncpg.connect("postgresql://{}:{}@postgres:{}/{}".format(
        POSTGRES_USER, POSTGRES_PASSWORD, POSTGRES_PORT, POSTGRES_DB
    ))

    query = """
    SELECT data FROM template_matrices WHERE name = $1 LIMIT 1
    """

    row = await conn.fetchrow(query, matrix_name)
    await conn.close()

    return np.array(row["data"]) if row else None

async def save_balance_matrix(chat_id, name, matrix):
    """Stores generated balance matrix in PostgreSQL."""
    logging.debug(f"Saving balance matrix for chatId: {chat_id}, name: {name}")
    conn = await asyncpg.connect("postgresql://{}:{}@postgres:{}/{}".format(
        POSTGRES_USER, POSTGRES_PASSWORD, POSTGRES_PORT, POSTGRES_DB
    ))

    query = """
    INSERT INTO matrices (chat_id, name, data) VALUES ($1, $2, $3)
    """

    await conn.execute(query, chat_id, name, matrix.tolist())
    await conn.close()

    await conn.close()
