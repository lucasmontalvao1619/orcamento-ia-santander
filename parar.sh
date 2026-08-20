#!/usr/bin/env bash
# Encerra o Orcamento IA. Os dados e o modelo ficam guardados nos volumes.
cd "$(dirname "$0")"
docker compose down
echo "Aplicacao encerrada. Seus lancamentos foram preservados."
echo "Para apagar tudo, inclusive os lancamentos: docker compose down -v"
