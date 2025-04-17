CREATE TABLE IF NOT EXISTS matrices
(
    id      SERIAL PRIMARY KEY,
    chat_id BIGINT,
    name    VARCHAR(100) NOT NULL,
    data    DOUBLE PRECISION[][]
);

CREATE INDEX IF NOT EXISTS idx_chat_id ON matrices (chat_id);

CREATE TABLE IF NOT EXISTS template_matrices
(
    id   SERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    data DOUBLE PRECISION[][]
);

INSERT INTO template_matrices (name, data)
VALUES ('player_attack',
        '{
        {0.8, 1.3, 0.0, 0.0, 0.0},
        {0.7, 0.8, 0.0, 1.1, 1.3},
        {1.1, 0.9, 0.7, 0.0, 1.2},
        {1.0, 1.0, 1.0, 1.0, 1.0}
        }'::double precision[][]);

INSERT INTO template_matrices (name, data)
VALUES ('monster_attack',
        '{
        {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
        {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
        {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
        {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.7, 0.7, 0.0, 0.0},
        {0.0, 0.0, 0.8, 0.0, 0.0, 0.0, 0.7, 0.8, 0.0, 0.0},
        {1.3, 1.2, 1.1, 0.0, 0.0, 0.0, 0.8, 0.7, 0.9, 1.3}
        }'::double precision[][]);

