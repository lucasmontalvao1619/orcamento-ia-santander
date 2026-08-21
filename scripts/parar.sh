#!/usr/bin/env bash
# Encerra o Orcamento IA. Os dados e o modelo ficam guardados nos volumes.
cd "$(dirname "$0")/.."

COMPOSE="docker compose -f docker/docker-compose.yml"

$COMPOSE down
echo "Aplicacao encerrada. Seus lancamentos foram preservados."
echo "Para apagar tudo, inclusive os lancamentos:"
echo "  docker compose -f docker/docker-compose.yml down -v"
