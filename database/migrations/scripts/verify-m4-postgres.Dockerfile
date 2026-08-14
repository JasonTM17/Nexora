# Disposable local verification image only: the M2 fixture pins PostgreSQL
# 17.5 and V024 needs the pgvector extension. This image is built on demand by
# verify-m4-schema-knowledge.ps1, never pushed, never used by production wiring.
FROM postgres:17.5-alpine

RUN apk add --no-cache git build-base postgresql17-dev \
    && git clone --depth 1 --branch v0.8.1 https://github.com/pgvector/pgvector.git /tmp/pgvector \
    && make -C /tmp/pgvector PG_CONFIG=/usr/local/bin/pg_config with_llvm=no \
    && make -C /tmp/pgvector PG_CONFIG=/usr/local/bin/pg_config with_llvm=no install \
    && apk del git build-base \
    && rm -rf /tmp/pgvector
