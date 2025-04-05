CREATE TABLE IF NOT EXISTS matrices
(
    id   SERIAL PRIMARY KEY,
    chat_id BIGINT,
    name TEXT,
    is_template BOOLEAN,
    data DOUBLE PRECISION[][]
);
