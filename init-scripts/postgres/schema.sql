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
        '{' ||
        '{0.0, 0.0, 0.0, 0.0, 0.0},' ||
        '{0.0, 0.0, 0.0, 0.0, 0.0},' ||
        '{0.0, 0.0, 0.0, 0.0, 0.0},' ||
        '{0.0, 0.0, 0.0, 0.0, 0.0}' ||
        '}');

INSERT INTO template_matrices (name, data)
VALUES ('monster_attack',
        '{' ||
        '{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},' ||
        '{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},' ||
        '{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},' ||
        '{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},' ||
        '{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},' ||
        '{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},' ||
        '}');

