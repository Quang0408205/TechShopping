#!/bin/bash
# Creates the test database and applies the same schema (source of truth: database/techshopping.sql)
set -e
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres -c "CREATE DATABASE techshopping_test;"
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname techshopping_test -f /docker-entrypoint-initdb.d/01_schema.sql
