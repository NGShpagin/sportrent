#!/bin/bash
# Idempotent: create/update per-service login roles + schemas for services that were added
# after the Postgres volume was first initialized (init-schemas.sh only runs on empty volume).
# Intended to run inside the db container on every deploy:
#   docker compose exec -T db bash < deploy/ensure-schemas.sh
# Passwords come from the container environment (same as init-schemas.sh).
set -euo pipefail

ensure_schema() { # <schema> <role> <password>
  local schema="$1" role="$2" pw="$3"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
       -v schema="$schema" -v role="$role" -v pw="$pw" -v db="$POSTGRES_DB" <<'SQL'
SELECT EXISTS(SELECT 1 FROM pg_roles WHERE rolname = :'role') AS role_exists \gset
\if :role_exists
ALTER ROLE :"role" LOGIN PASSWORD :'pw';
\else
CREATE ROLE :"role" LOGIN PASSWORD :'pw';
\endif

SELECT EXISTS(SELECT 1 FROM pg_namespace WHERE nspname = :'schema') AS schema_exists \gset
\if :schema_exists
ALTER SCHEMA :"schema" OWNER TO :"role";
\else
CREATE SCHEMA :"schema" AUTHORIZATION :"role";
\endif

ALTER ROLE :"role" SET search_path = :"schema";
GRANT CONNECT ON DATABASE :"db" TO :"role";
SQL
  echo "ensured schema=$schema role=$role"
}

ensure_schema cart cart_svc "${CART_DB_PASSWORD:?}"
ensure_schema orders orders_svc "${ORDER_DB_PASSWORD:?}"
ensure_schema promocodes promocodes_svc "${PROMOCODE_DB_PASSWORD:?}"
