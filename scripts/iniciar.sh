#!/usr/bin/env bash
# Sobe o Orcamento IA.
#
# Ha dois caminhos, e o script escolhe sozinho:
#
#   1. Docker      nao exige Java, Maven nem Ollama na maquina. E o caminho de
#                  quem so recebeu o zip, e por isso vem primeiro.
#   2. Local       usado quando o Docker nao esta disponivel mas a maquina ja
#                  tem Java e Ollama. Faz exatamente a mesma coisa, sem container.
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
    echo "O Orcamento IA ja esta rodando."
    anunciar_pronto
    if command -v open > /dev/null 2>&1; then open "$URL"; fi
    exit 0
fi

# --- Caminho 1: Docker ------------------------------------------------------
if docker info > /dev/null 2>&1; then
    echo "Iniciando o Orcamento IA (Docker)..."
    echo "Na primeira vez o modelo de IA e baixado (~1,9 GB); pode levar alguns minutos."
    echo ""
    docker compose -f docker/docker-compose.yml up --build -d

    echo ""
    echo "Aguardando a aplicacao ficar pronta..."
    esperar_e_abrir
    echo "Para parar: scripts/parar.sh"
    exit 0
fi

# --- Caminho 2: local, sem container ----------------------------------------
if command -v java > /dev/null 2>&1 && command -v ollama > /dev/null 2>&1; then
    echo "O Docker nao esta disponivel, mas esta maquina tem Java e Ollama."
    echo "Subindo em modo local, sem container."
    echo ""

    # O Ollama e um processo a parte: sem ele no ar, o assistente responde 503.
    if ! curl -s -o /dev/null -m 3 http://localhost:11434/api/tags 2>/dev/null; then
        echo "Iniciando o Ollama..."
        nohup ollama serve > /dev/null 2>&1 &
        until curl -s -o /dev/null -m 2 http://localhost:11434/api/tags 2>/dev/null; do sleep 2; done
    fi

    MODELO="${OLLAMA_MODEL:-qwen2.5:3b}"
    if ! ollama list 2>/dev/null | grep -q "^$MODELO"; then
        echo "Baixando o modelo $MODELO (~1,9 GB, apenas na primeira vez)..."
        ollama pull "$MODELO"
    fi

    # Abre o navegador de um processo paralelo: o Maven fica em primeiro plano
    # para os logs aparecerem e o Ctrl+C encerrar a aplicacao.
    ( esperar_e_abrir ; echo "Para parar: Ctrl+C nesta janela." ) &

    exec ./mvnw spring-boot:run
fi

# --- Nenhum dos dois --------------------------------------------------------
echo "Nao foi possivel iniciar: falta o Docker."
if command -v colima > /dev/null 2>&1; then
    echo "Esta maquina tem o colima instalado. Suba o daemon com:  colima start"
else
    echo "Abra o Docker Desktop e execute este arquivo de novo."
fi
echo ""
echo "Alternativa sem Docker: instale o Java 17+ e o Ollama (brew install ollama)."
exit 1
