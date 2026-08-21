#!/usr/bin/env bash
# Sobe o Orcamento IA. Requer apenas o Docker instalado e aberto.
#
# Este script vive em scripts/, mas trabalha a partir da raiz do projeto: e la
# que estao o pom.xml e o src que o build dentro do container copia.
set -e
cd "$(dirname "$0")/.."

COMPOSE="docker compose -f docker/docker-compose.yml"

if ! docker info > /dev/null 2>&1; then
    echo "O Docker nao esta rodando."
    echo "Abra o Docker Desktop e execute este arquivo de novo."
    exit 1
fi

echo "Iniciando o Orcamento IA..."
echo "Na primeira vez o modelo de IA e baixado (~4,7 GB); pode levar alguns minutos."
echo ""
$COMPOSE up --build -d

echo ""
echo "Aguardando a aplicacao ficar pronta..."
until curl -s -o /dev/null http://localhost:8080/api/sobre 2>/dev/null; do sleep 3; done

# IP na rede local: o celular nao enxerga "localhost", precisa do endereco da
# maquina. Instalar como app no celular so funciona por este endereco.
IP=$(ipconfig getifaddr en0 2>/dev/null || hostname -I 2>/dev/null | awk '{print $1}')

echo ""
echo "Pronto."
echo "  Neste computador:  http://localhost:8080"
if [ -n "$IP" ]; then
    echo "  No celular:        http://$IP:8080"
    echo "                     (mesma rede Wi-Fi; no navegador, use"
    echo "                      'Adicionar a tela de inicio' para instalar)"
fi
echo ""
echo "Para parar: scripts/parar.sh"

if command -v open > /dev/null 2>&1; then open http://localhost:8080; fi
