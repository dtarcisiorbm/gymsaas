#!/bin/sh
# Runs once on first database initialization (as POSTGRES_USER = gymsaas_owner).
# Creates the non-superuser runtime role used by the application. RLS is only
# meaningful for non-superusers, therefore the backend never connects as owner.
set -e

APP_USER="${APP_DB_USER:-gymsaas_app}"
APP_PASSWORD="${APP_DB_PASSWORD:-gymsaas_app_pw}"
DB_NAME="${POSTGRES_DB:-gymsaas}"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$DB_NAME" <<-EOSQL
    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${APP_USER}') THEN
            CREATE ROLE ${APP_USER} LOGIN PASSWORD '${APP_PASSWORD}'
                NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOBYPASSRLS;
        END IF;
    END
    \$\$;

    GRANT CONNECT ON DATABASE ${DB_NAME} TO ${APP_USER};
    GRANT USAGE ON SCHEMA public TO ${APP_USER};

    -- Objects created by Flyway (owner) are automatically usable by the runtime role.
    ALTER DEFAULT PRIVILEGES FOR ROLE ${POSTGRES_USER} IN SCHEMA public
        GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO ${APP_USER};
    ALTER DEFAULT PRIVILEGES FOR ROLE ${POSTGRES_USER} IN SCHEMA public
        GRANT USAGE, SELECT ON SEQUENCES TO ${APP_USER};
EOSQL

echo "Runtime role ${APP_USER} provisioned (NOBYPASSRLS)."
