CREATE TABLE IF NOT EXISTS matrices
(
    id      SERIAL PRIMARY KEY,
    chat_id BIGINT,
    name    VARCHAR(100) NOT NULL,
    data    DOUBLE PRECISION[][]
);

CREATE INDEX IF NOT EXISTS idx_chat_id ON matrices (chat_id);
ALTER TABLE matrices ADD CONSTRAINT unique_chat_matrix UNIQUE (chat_id, name);

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

INSERT INTO template_matrices (name, data)
VALUES ('weapon_type_attr',
        '{
        {6.0, 8.0, 4.0, 8.0, 8.0, 5.0, 7.0},
        {0.3, 0.3, 0.4, 0.2, 0.3, 0.3, 0.3},
        {1.2, 1.1, 1.05, 1.2, 1.1, 1.3, 1.1},
        {0.5, 0.7, 0.4, 0.5, 0.6, 0.2, 0.6},
        {0.05, 0.1, 0.05, 0.4, 0.2, 0.05, 0.2}
        }'::double precision[][]);

INSERT INTO template_matrices (name, data)
VALUES ('weapon_handling_type_adjustment',
        '{
        {0.0, 0.0, 0.95, 1.15},
        {0.0, 0.0, 1.05, 0.95},
        {0.0, 0.0, 1.1, 1.2},
        {0.0, 0.0, 0.9, 1.1},
        {0.0, 0.0, 1.0, 1.0}}'::double precision[][]);

INSERT INTO template_matrices (name, data)
VALUES ('weapon_material_adjustment',
        '{
        {0.0, 1.2, 1.1, 1.1, 0.0, 1.3, 1.5, 1.4, 1.3, 1.1},
        {0.0, 1.05, 1.1, 1.05, 1.3, 1.3, 1.5, 1.4, 1.2, 1.2},
        {1.05, 0.0, 0.0, 0.0, 1.2, 1.3, 1.4, 1.5, 1.5, 1.3},
        {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
        {0.9, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}}'::double precision[][]);

INSERT INTO template_matrices (name, data)
VALUES ('weapon_handler_material_adjustment',
        '{
        {0.0, 0.0, 0.0, 0.0, 1.3},
        {0.0, 1.1, 1.05, 1.15, 1.2},
        {0.0, 0.0, 1.1, 1.2, 1.3},
        {0.0, 0.9, 0.0, 0.7, 0.0},
        {0.0, 0.0, 0.0, 0.0, 0.0}}'::double precision[][]);

INSERT INTO template_matrices (name, data)
VALUES ('weapon_complete_wood_adjustment',
        '{
        {0.0},
        {0.0},
        {0.0},
        {0.0},
        {1.1}}'::double precision[][]);

INSERT INTO template_matrices (name, data)
VALUES ('weapon_complete_steel_adjustment',
        '{
        {1.1},
        {0.0},
        {0.0},
        {0.0},
        {0.0}}'::double precision[][]);

INSERT INTO template_matrices (name, data)
VALUES ('weapon_complete_dragon_bone_adjustment',
        '{
        {1.5},
        {0.3},
        {1.7},
        {1.6},
        {0.0}}'::double precision[][]);

INSERT INTO template_matrices (name, data)
VALUES ('weapon_size_adjustment',
        '{
        {0.9, 1.0, 1.2},
        {1.0, 1.0, 1.0},
        {1.0, 1.0, 1.0},
        {0.8, 0.9, 1.15},
        {1.0, 1.0, 1.0}}'::double precision[][]);

INSERT INTO template_matrices (name, data)
VALUES ('weapon_attack_type_adjustment',
        '{
        {0.0, 1.2, 0.0, 1.0},
        {1.3, 0.0, 0.0, 1.0},
        {0.0, 1.2, 0.0, 1.0},
        {0.0, 0.0, 0.0, 1.0},
        {0.0, 0.0, 1.4, 1.0}}'::double precision[][]);

INSERT INTO template_matrices (name, data)
VALUES ('items_quality_adjustment',
        '{
        {0.8},
        {1.1},
        {1.3},
        {1.4},
        {1.6}
        }'::double precision[][]);

INSERT INTO template_matrices (name, data)
VALUES ('wearable_armor_bonus',
        '{
        {-2.0},
        {-1.0},
        {1.0},
        {2.0},
        {3.0},
        {4.0},
        {-1.0},
        {4.0},
        {0.0},
        {0.0}
        }'::double precision[][]);

INSERT INTO template_matrices (name, data)
VALUES ('wearable_chance_to_dodge_adjustment',
        '{
        {0.0},
        {0.3},
        {0.4},
        {0.2},
        {0.1},
        {0.0},
        {0.3},
        {0.0},
        {0.0},
        {0.0}
        }'::double precision[][]);