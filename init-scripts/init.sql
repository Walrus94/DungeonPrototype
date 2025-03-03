CREATE TABLE IF NOT EXISTS matrices_prod
(
    id   SERIAL PRIMARY KEY,
    char_id BIGINT,
    name TEXT,
    is_template BOOLEAN,
    data DOUBLE PRECISION[][]
);
CREATE TABLE IF NOT EXISTS matrices_dev
(
    id   SERIAL PRIMARY KEY,
    char_id BIGINT,
    name TEXT,
    is_template BOOLEAN,
    data DOUBLE PRECISION[][]
);
CREATE TABLE IF NOT EXISTS matrices_test
(
    id   SERIAL PRIMARY KEY,
    char_id BIGINT,
    name TEXT,
    is_template BOOLEAN,
    data DOUBLE PRECISION[][]
);
