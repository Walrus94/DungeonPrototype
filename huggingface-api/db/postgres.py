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
    INSERT INTO matrices (chat_id, name, data) 
    VALUES ($1, $2, $3)
    ON CONFLICT (chat_id, name) 
    DO UPDATE SET 
        data = $3
    """

    try:
        await conn.execute(query, chat_id, name, matrix.tolist())
        logging.debug(f"Successfully saved/updated matrix {name} for chat {chat_id}")
    except Exception as e:
        logging.error(f"Error saving matrix: {e}")
        raise
    finally:
        await conn.close()

async def load_balance_matrix(chat_id, name):
    """Loads balance matrix from PostgreSQL."""
    logging.debug(f"Loading balance matrix for chatId: {chat_id}, name: {name}")
    conn = await asyncpg.connect("postgresql://{}:{}@postgres:{}/{}".format(
        POSTGRES_USER, POSTGRES_PASSWORD, POSTGRES_PORT, POSTGRES_DB
    ))

    query = """
    SELECT data FROM matrices WHERE chat_id = $1 AND name = $2 LIMIT 1
    """

    row = await conn.fetchrow(query, chat_id, name)
    await conn.close()

    return np.array(row["data"]) if row else None

async def load_chat_matrices(chat_id: int) -> dict:
    """Load all balance matrices associated with a chat."""
    conn = await asyncpg.connect("postgresql://{}:{}@postgres:{}/{}".format(
        POSTGRES_USER, POSTGRES_PASSWORD, POSTGRES_PORT, POSTGRES_DB
    ))
    try:
        async with conn.cursor() as cur:
            await cur.execute(
                """
                SELECT name, data 
                FROM balance_matrices 
                WHERE chat_id = %s
                """,
                    (chat_id,)
            )
            results = await cur.fetchall()
            return {row[0]: row[1] for row in results} if results else {}
    except Exception as e:
        logging.error(f"Error loading chat matrices: {e}")
        return {}
