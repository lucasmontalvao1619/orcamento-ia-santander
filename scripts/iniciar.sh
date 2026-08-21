#!/usr/bin/env bash
# Sobe o Fast Finance Helper.
#
# Ha dois caminhos, e o script escolhe sozinho:
#
#   1. Local       quando a maquina tem Java. E o caminho direto.
#   2. Docker      quando nao tem: funciona sem instalar nada.
#
# A escolha automatica existe porque "o Docker nao esta rodando" e uma resposta
# inutil para quem tem todas as pecas instaladas e so queria abrir o aplicativo.
#
# Este script vive em scripts/, mas trabalha a partir da raiz do projeto: e la
# que estao o pom.xml e o src.
set -e
cd "$(dirname "$0")/.."

PORTA=8080
URL="http://localhost:$PORTA"

# O celular nao enxerga "localhost": precisa do endereco da maquina na rede.
# Instalar como app no celular so funciona por este endereco.
endereco_local() {
    ipconfig getifaddr en0 2>/dev/null || hostname -I 2>/dev/null | awk '{print $1}'
}

anunciar_pronto() {
    local ip
    ip=$(endereco_local)
    echo ""
    echo "Pronto."
    echo "  Neste computador:  $URL"
    if [ -n "$ip" ]; then
        echo "  No celular:        http://$ip:$PORTA"
        echo "                     (mesma rede Wi-Fi; no navegador, use"
        echo "                      'Adicionar a tela de inicio' para instalar)"
    fi
    echo ""
}

esperar_e_abrir() {
    until curl -s -o /dev/null "$URL/api/sobre" 2>/dev/null; do sleep 3; done
    anunciar_pronto
    if command -v open > /dev/null 2>&1; then open "$URL"; fi
}

if curl -s -o /dev/null -m 2 "$URL/api/sobre" 2>/dev/null; then
    echo "O Fast Finance Helper ja esta rodando."
    anunciar_pronto
    if command -v open > /dev/null 2>&1; then open "$URL"; fi
    exit 0
fi

# --- Caminho 1: local, sem container ----------------------------------------
# O local vem primeiro: sobe em segundos e nao depende de o Docker estar aberto.
if command -v java > /dev/null 2>&1; then
    echo "Subindo em modo local."
    echo ""

    # Abre o navegador de um processo paralelo: o Maven fica em primeiro plano
    # para os logs aparecerem e o Ctrl+C encerrar a aplicacao.
    ( esperar_e_abrir ; echo "Para parar: Ctrl+C nesta janela." ) &

    exec ./mvnw spring-boot:run
fi


# --- Caminho 2: Docker ------------------------------------------------------
# Para quem nao tem Java instalado: funciona sem instalar nada.
if docker info > /dev/null 2>&1; then
    echo "Iniciando o Fast Finance Helper (Docker)..."
    echo ""
    docker compose -f docker/docker-compose.yml up --build -d

    echo ""
    echo "Aguardando a aplicacao ficar pronta..."
    esperar_e_abrir
    echo "Para parar: scripts/parar.sh"
    exit 0
fi

# --- Nenhum dos dois --------------------------------------------------------
echo "Nao foi possivel iniciar: falta o Docker."
if command -v colima > /dev/null 2>&1; then
    echo "Esta maquina tem o colima instalado. Suba o daemon com:  colima start"
else
    echo "Abra o Docker Desktop e execute este arquivo de novo."
fi
echo ""
echo "Alternativa sem Docker: instale o Java 17 ou mais novo."
exit 1
