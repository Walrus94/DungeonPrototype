#!/bin/bash
set -e

# Use the PostgreSQL user from environment variables
POSTGRES_USER="${POSTGRES_DATABASE_USER:-postgres}"

DATABASES=("balance_matrices_dev" "balance_matrices_prod" "balance_matrices_test")

for db in "${DATABASES[@]}"; do
  echo "Creating database: $db"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE $db;
    GRANT ALL PRIVILEGES ON DATABASE $db TO $POSTGRES_USER;
EOSQL
  echo "Applying schema to database: $db"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname="$db" -f /docker-entrypoint-initdb.d/schema.sql
done
