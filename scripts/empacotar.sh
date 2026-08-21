#!/usr/bin/env bash
# Gera o zip distribuivel do Fast Finance Helper.
# O pacote sai limpo: sem build, sem dados e sem historico do git — assim quem
# extrair comeca com o orcamento zerado.
set -e
cd "$(dirname "$0")/.."

NOME="orcamento-ia"
SAIDA="dist"
PACOTE="$SAIDA/$NOME.zip"

rm -rf "$SAIDA"
mkdir -p "$SAIDA"

zip -r "$PACOTE" . \
    -x "target/*" \
    -x "dados/*" \
    -x "dist/*" \
    -x "logs/*" \
    -x ".git/*" \
    -x ".idea/*" \
    -x ".claude/*" \
    -x ".vscode/*" \
    -x "*.iml" \
    -x ".DS_Store" \
    -x "*/.DS_Store" \
    -x ".mvn/wrapper/maven-wrapper.jar" \
    > /dev/null

echo "Pacote gerado: $PACOTE  ($(du -h "$PACOTE" | cut -f1))"
echo ""
echo "Quem receber precisa apenas do Docker instalado:"
echo "  macOS    extrair e dar dois cliques em 'Iniciar Fast Finance Helper'"
echo "           (na primeira vez: clique com o botao direito > Abrir)"
echo "  Windows  extrair e dar dois cliques em 'Iniciar Fast Finance Helper.bat'"
echo "  Terminal scripts/iniciar.sh"
