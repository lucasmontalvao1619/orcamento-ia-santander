#!/usr/bin/env bash
# Gera o zip distribuivel do Orcamento IA.
# O pacote sai limpo: sem build, sem dados e sem historico do git — assim quem
# extrair comeca com o orcamento zerado.
set -e
cd "$(dirname "$0")"

NOME="orcamento-ia"
SAIDA="dist"
PACOTE="$SAIDA/$NOME.zip"

rm -rf "$SAIDA"
mkdir -p "$SAIDA"

zip -r "$PACOTE" . \
    -x "target/*" \
    -x "dados/*" \
    -x "dist/*" \
    -x ".git/*" \
    -x ".idea/*" \
    -x "*.iml" \
    -x ".DS_Store" \
    -x "*/.DS_Store" \
    -x ".mvn/wrapper/maven-wrapper.jar" \
    > /dev/null

echo "Pacote gerado: $PACOTE  ($(du -h "$PACOTE" | cut -f1))"
echo ""
echo "Quem receber precisa apenas do Docker instalado:"
echo "  1. Extrair o zip"
echo "  2. Rodar ./iniciar.sh  (ou iniciar.bat no Windows)"
echo "  3. Abrir http://localhost:8080"
