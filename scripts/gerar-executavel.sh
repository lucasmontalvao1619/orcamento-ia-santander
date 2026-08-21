#!/usr/bin/env bash
# Gera o executavel de duplo clique com o Java embutido dentro dele.
#
# Diferenca para os atalhos da raiz: aqueles chamam o Maven da maquina, entao
# exigem Java e Maven instalados. O que sai daqui nao exige nada — o jpackage
# empacota uma JVM junto. E o formato que da para mandar para outra pessoa.
#
# jpackage so gera para o sistema em que roda: no macOS sai um .app, no Windows
# um .exe. Por isso o Windows e construido pelo GitHub Actions
# (.github/workflows/executaveis.yml), que tem uma maquina Windows de verdade.
set -e
cd "$(dirname "$0")/.."

NOME="Fast Finance Helper"
# A primeira <version> do pom e a do parent (Spring Boot). A do projeto vem
# depois do </parent> — sem isso o app sai carimbado com a versao do framework.
VERSAO=$(sed -n '/<\/parent>/,$p' pom.xml | grep -m1 '<version>' | sed 's/.*<version>\(.*\)<\/version>.*/\1/')
SAIDA="dist/executavel"
PALCO="target/palco-jpackage"

if ! command -v jpackage > /dev/null 2>&1; then
    echo "jpackage nao encontrado. Ele vem com o JDK 17 ou mais novo."
    echo "Verifique com: java -version"
    exit 1
fi

echo "Compilando o jar..."
./mvnw -q clean package -DskipTests

JAR=$(ls target/*.jar | head -1)
JAR_NOME=$(basename "$JAR")

# O jpackage copia TUDO que estiver em --input para dentro do app. Sem uma pasta
# so com o jar, o target inteiro (classes, relatorios de teste) iria junto.
rm -rf "$PALCO" "$SAIDA"
mkdir -p "$PALCO" "$SAIDA"
cp "$JAR" "$PALCO/"

echo "Empacotando com o Java embutido..."
# Sem --add-modules o jpackage embute o JDK inteiro. Esta lista sai do jdeps
# mais os modulos que ele nao enxerga: Spring e Hibernate carregam muita coisa
# por reflexao, e um modulo faltando so aparece como erro em tempo de execucao.
# Enxugar a JVM e o maior ganho de tamanho disponivel sem mexer no codigo.
MODULOS="java.base,java.compiler,java.desktop,java.instrument,java.logging,\
java.management,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,\
java.security.jgss,java.security.sasl,java.sql,java.sql.rowset,\
java.transaction.xa,java.xml,java.xml.crypto,jdk.crypto.ec,jdk.crypto.cryptoki,\
jdk.httpserver,jdk.jfr,jdk.management,jdk.net,jdk.unsupported,jdk.zipfs"

jpackage \
    --type app-image \
    --add-modules "$MODULOS" \
    --name "$NOME" \
    --app-version "$VERSAO" \
    --input "$PALCO" \
    --main-jar "$JAR_NOME" \
    --icon "scripts/icone.icns" \
    --java-options "-Dapp.empacotado=true" \
    --java-options "-Dapp.abrir-navegador=true" \
    --java-options "-Dapp.encerrar-ao-fechar=true" \
    --dest "$SAIDA"

echo ""
echo "Pronto: $SAIDA/$NOME.app"
echo ""
echo "Este executavel nao precisa de Java nem Maven na maquina."
echo "Os lancamentos ficam em ~/.orcamento-ia, e nao dentro do aplicativo."
echo "O assistente de voz continua exigindo o Ollama rodando."
