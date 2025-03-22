import asyncpg
from config.settings import POSTGRES_USER, POSTGRES_PASSWORD, POSTGRES_DB, POSTGRES_PORT

async def load_template_matrix(chat_id, database):
    """Loads predefined template matrix from PostgreSQL."""
    conn = await asyncpg.connect("postgresql://{}:{}@postgres:{}/{}".format(
        POSTGRES_USER, POSTGRES_PASSWORD, POSTGRES_PORT, database
    ))

    row = await conn.fetchrow("SELECT data FROM {} WHERE chat_id = {} and is_template = false LIMIT 1".format(database, chat_id))
    await conn.close()

    return np.array(row["data"]) if row else None

async def save_balance_matrix(chat_id, name, matrix):
    """Stores generated balance matrix in PostgreSQL."""
    conn = await asyncpg.connect("postgresql://{}:{}@postgres:{}/{}".format(
        POSTGRES_USER, POSTGRES_PASSWORD, POSTGRES_PORT, POSTGRES_DB
    ))

    await conn.execute(
        "INSERT INTO $1 (chat_id, name, data) VALUES ($2, $3, $4)",
        POSTGRES_DB, chat_id, name, matrix.tolist()
    )

    await conn.close()
