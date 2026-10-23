#!/bin/sh
set -eu

cd "$(dirname "$0")"
if [ ! -t 0 ]; then
    docker login ghcr.io --username pikahk --password-stdin
fi
docker compose pull api
docker compose up -d
docker image prune -f
