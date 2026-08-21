#!/usr/bin/env bash
# Encerra o Fast Finance Helper, tenha ele subido por container ou em modo local.
cd "$(dirname "$0")/.."

PORTA=8080
parou=0

# --- Modo Docker ------------------------------------------------------------
if docker info > /dev/null 2>&1; then
    if docker compose -f docker/docker-compose.yml ps --quiet 2>/dev/null | grep -q .; then
        docker compose -f docker/docker-compose.yml down
        parou=1
        echo "Containers encerrados. Os dados e o modelo ficam guardados nos volumes."
        echo "Para apagar tudo, inclusive os lancamentos:"
        echo "  docker compose -f docker/docker-compose.yml down -v"
    fi
fi

# --- Modo local -------------------------------------------------------------
# O processo fica escutando na porta; e por ela que o encontramos, em vez de
# adivinhar pelo nome do comando. O -sTCP:LISTEN e obrigatorio: sem ele o lsof
# devolve tambem quem esta CONECTADO na porta — o navegador do usuario com a
# aplicacao aberta — e o kill levaria o navegador junto.
PIDS=$(lsof -ti:"$PORTA" -sTCP:LISTEN 2>/dev/null)
if [ -n "$PIDS" ]; then
    # shellcheck disable=SC2086
    kill $PIDS 2>/dev/null
    # Da um tempo para o encerramento limpo antes de insistir: o H2 precisa
    # fechar o arquivo do banco direito.
    for _ in 1 2 3 4 5; do
        sleep 1
        lsof -ti:"$PORTA" -sTCP:LISTEN > /dev/null 2>&1 || break
    done
    lsof -ti:"$PORTA" -sTCP:LISTEN 2>/dev/null | xargs kill -9 2>/dev/null
    parou=1
    echo "Aplicacao encerrada. Seus lancamentos foram preservados."
fi

if [ "$parou" -eq 0 ]; then
    echo "A aplicacao nao estava rodando."
fi
