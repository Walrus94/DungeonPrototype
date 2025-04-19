import asyncpg
import logging
from config.settings import POSTGRES_USER, POSTGRES_PASSWORD, POSTGRES_DB, POSTGRES_PORT

async def load_template_matrix(chat_id, database, matrix_name):
    """Loads predefined template matrix from PostgreSQL."""
    logging.debug(f"Loading template matrix for chatId: {chat_id}, name: {matrix_name}, database: {database}")
    conn = await asyncpg.connect("postgresql://{}:{}@postgres:{}/{}".format(
        POSTGRES_USER, POSTGRES_PASSWORD, POSTGRES_PORT, POSTGRES_DB
    ))

    query = """
    SELECT data FROM "{}".template_matrices WHERE chat_id = $1 and name = $2 LIMIT 1
    """.format(database)

    row = await conn.fetchrow(query, chat_id, matrix_name)
    await conn.close()

    return np.array(row["data"]) if row else None

async def save_balance_matrix(chat_id, database, name, matrix):
    """Stores generated balance matrix in PostgreSQL."""
    logging.debug(f"Saving balance matrix for chatId: {chat_id}, name: {name}, database: {database}")
    conn = await asyncpg.connect("postgresql://{}:{}@postgres:{}/{}".format(
        POSTGRES_USER, POSTGRES_PASSWORD, POSTGRES_PORT, POSTGRES_DB
    ))

    query = """
    INSERT INTO "{}".template_matrices (chat_id, name, data) VALUES ($1, $2, $3)
    """.format(database)

    await conn.execute(query, chat_id, name, matrix.tolist())
    await conn.close()

    await conn.close()
