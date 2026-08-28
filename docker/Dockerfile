# --- Etapa 1: compila o jar -------------------------------------------------
# O build acontece dentro da imagem para que quem for rodar nao precise ter
# Java nem Maven instalados na maquina.
FROM maven:3.9-eclipse-temurin-17 AS construcao
WORKDIR /construcao

# As dependencias vem primeiro e em camada propria: enquanto o pom.xml nao
# mudar, o Docker reaproveita o cache e nao rebaixa tudo a cada alteracao.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# --- Etapa 2: imagem final --------------------------------------------------
# Só o JRE e o jar: a imagem final nao carrega o Maven nem o codigo-fonte.
FROM eclipse-temurin:17-jre
WORKDIR /app

# Usuario sem privilegios: o app nao tem motivo para rodar como root.
RUN useradd --create-home --shell /bin/bash orcamento
COPY --from=construcao /construcao/target/*.jar app.jar

# O banco H2 fica num volume para os lancamentos sobreviverem ao restart.
RUN mkdir -p /app/dados && chown -R orcamento:orcamento /app
USER orcamento

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
