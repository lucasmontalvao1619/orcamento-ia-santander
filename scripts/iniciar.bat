@echo off
REM Sobe o Orcamento IA no Windows. Requer apenas o Docker Desktop instalado.
REM Trabalha a partir da raiz do projeto, uma pasta acima desta.
cd /d "%~dp0.."

docker info >nul 2>&1
if errorlevel 1 (
    echo O Docker nao esta rodando.
    echo Abra o Docker Desktop e execute este arquivo de novo.
    pause
    exit /b 1
)

echo Iniciando o Orcamento IA...
echo Na primeira vez o modelo de IA e baixado ^(~1,9 GB^); pode levar alguns minutos.
echo.
docker compose -f docker/docker-compose.yml up --build -d

echo.
echo Aguardando a aplicacao ficar pronta...
:esperar
timeout /t 3 /nobreak >nul
curl -s -o nul http://localhost:8080/api/sobre 2>nul
if errorlevel 1 goto esperar

echo.
echo Pronto. Abra no navegador: http://localhost:8080
start http://localhost:8080
pause
