FROM postgres:11.0

COPY init-db.sh /docker-entrypoint-initdb.d

HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD psql -c 'select 1' -d bar -U $BAR_DB_USERNAME

EXPOSE 5432
