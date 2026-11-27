#!/bin/bash
# One database, one schema per service. Each service gets its own login role that owns only its
# schema, so a service cannot read or modify another service's tables.
#
# Stub services (catalog) get an empty schema only — no login role yet,
# because those services are not wired to Postgres.
#
# Also creates the "readonly" role (SELECT on all schemas).
# Runs once, when the data volume is empty (postgres image /docker-entrypoint-initdb.d).
# Passwords come from the container environment (docker-compose.yml / docker-compose.prod.yml).
set -euo pipefail

create_schema() { # <schema> <role> <password>
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
       -v schema="$1" -v role="$2" -v pw="$3" -v db="$POSTGRES_DB" <<'SQL'
CREATE ROLE :"role" LOGIN PASSWORD :'pw';
CREATE SCHEMA :"schema" AUTHORIZATION :"role";
ALTER ROLE :"role" SET search_path = :"schema";
GRANT CONNECT ON DATABASE :"db" TO :"role";
SQL
}

# Empty placeholder schemas for services that do not persist data yet ("order" is reserved → "orders").
create_empty_schema() { # <schema>
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
       -v schema="$1" <<'SQL'
CREATE SCHEMA :"schema";
SQL
}

# nobody but the owner gets implicit access to the database or the public schema
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -v db="$POSTGRES_DB" <<'SQL'
REVOKE ALL ON DATABASE :"db" FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
SQL

create_schema users users_svc "${USERS_DB_PASSWORD:?}"
create_schema auth  auth_svc  "${AUTH_DB_PASSWORD:?}"
create_schema authz authz_svc "${AUTHZ_DB_PASSWORD:?}"
create_schema cart cart_svc "${CART_DB_PASSWORD:?}"
create_schema orders orders_svc "${ORDER_DB_PASSWORD:?}"
create_schema promocodes promocodes_svc "${PROMOCODE_DB_PASSWORD:?}"

create_empty_schema catalog

# Read-only access to every schema (current and future tables) for reporting / support / debugging.
# pg_read_all_data = SELECT on all tables, views and sequences; default_transaction_read_only is a second
# safety net so even an explicit "BEGIN READ WRITE" is rejected. Note that it can read everything,
# including auth.refresh_tokens and password hashes - hand it out accordingly.
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
     -v role=readonly -v pw="${READONLY_DB_PASSWORD:?}" -v db="$POSTGRES_DB" <<'SQL'
CREATE ROLE :"role" LOGIN PASSWORD :'pw' NOSUPERUSER NOCREATEDB NOCREATEROLE;
GRANT pg_read_all_data TO :"role";
GRANT CONNECT ON DATABASE :"db" TO :"role";
ALTER ROLE :"role" SET default_transaction_read_only = on;
SQL
