import asyncpg
from config.settings import POSTGRES_USER, POSTGRES_PASSWORD, POSTGRES_DB, POSTGRES_PORT

async def load_template_matrix():
    """Loads predefined template matrix from PostgreSQL."""
    conn = await asyncpg.connect("postgresql://{}:{}@postges:{}/{}".format(
        POSTGRES_USER, POSTGRES_PASSWORD, POSTGRES_PORT, POSTGRES_DB
    ))

    row = await conn.fetchrow("SELECT data FROM balance_matrices WHERE chat_id = 'TEMPLATE' LIMIT 1")
    await conn.close()

    return np.array(row["data"]) if row else None

async def save_balance_matrix(chat_id, name, matrix):
    """Stores generated balance matrix in PostgreSQL."""
    conn = await asyncpg.connect("postgresql://{}:{}@postges:{}/{}".format(
        POSTGRES_USER, POSTGRES_PASSWORD, POSTGRES_PORT, POSTGRES_DB
    ))

    await conn.execute(
        "INSERT INTO balance_matrices (chat_id, name, data) VALUES ($1, $2, $3)",
        chat_id, name, matrix.tolist()
    )

    await conn.close()
