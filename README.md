# Orcamento IA - Assistente Financeiro por Voz

API de orcamento pessoal construida com **Spring Boot** e **Spring AI**. O usuario envia um comando de voz (arquivo de audio) e a aplicacao transcreve o audio, interpreta a intencao com um modelo de linguagem e executa a acao real correspondente: registrar uma receita/despesa ou consultar saldo, gastos e transacoes.

## O que o projeto faz

O fluxo principal segue estes passos:

1. O cliente envia um arquivo de audio para a API.
2. O audio e transcrito para texto (Whisper, da OpenAI).
3. O texto e enviado ao modelo de linguagem atraves do `ChatClient`.
4. O modelo decide, via **Tool Calling**, qual funcao da aplicacao executar.
5. A funcao registra ou consulta transacoes no banco de dados.
6. Uma resposta em linguagem natural e devolvida ao usuario.

## Tecnologias utilizadas

- Java 17
- Spring Boot 3.3
- Spring AI 1.0.0-M3 (ChatClient, Tool Calling, transcricao de audio)
- Spring Data JPA
- Banco de dados H2 (em memoria)
- Bean Validation
- SpringDoc / Swagger UI
- JUnit 5

## Como executar

Pre-requisitos: **Java 17**, **Maven** e uma chave de API da OpenAI.

Defina a chave em uma variavel de ambiente:

```bash
# Linux / macOS
export OPENAI_API_KEY="sua-chave-aqui"

# Windows (PowerShell)
$env:OPENAI_API_KEY="sua-chave-aqui"
```

Execute a aplicacao:

```bash
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`.

## Como testar o fluxo principal

### 1. Enviar um comando de voz

Grave um audio dizendo algo como *"Registrar uma despesa de 50 reais com almoco na categoria alimentacao"* e envie:

```bash
curl -X POST http://localhost:8080/api/assistente/audio \
  -F "arquivo=@comando.mp3"
```

Resposta:

```json
{
  "textoTranscrito": "Registrar uma despesa de 50 reais com almoco na categoria alimentacao",
  "respostaAssistente": "Transacao registrada com sucesso. Despesa de R$ 50,00 na categoria alimentacao."
}
```

### 2. Enviar um comando por texto (util para testar sem audio)

```bash
curl -X POST http://localhost:8080/api/assistente/texto \
  --data-urlencode "comando=Qual e o meu saldo atual?"
```

### 3. Endpoints REST diretos

| Metodo | Rota | Descricao |
|--------|------|-----------|
| POST | `/api/assistente/audio` | Processa um comando de voz |
| POST | `/api/assistente/texto` | Processa um comando de texto |
| POST | `/api/transacoes` | Cria uma transacao manualmente |
| GET | `/api/transacoes` | Lista transacoes (filtro opcional por `tipo`) |
| GET | `/api/transacoes/saldo` | Retorna o saldo atual |

## Melhoria implementada

Alem do fluxo base de registrar transacoes por voz, esta versao evoluiu o projeto com:

- **Novas ferramentas de Tool Calling:** consulta de saldo, consulta de gasto por categoria e listagem de transacoes, permitindo que o assistente responda a mais tipos de pergunta.
- **Validacao antes de salvar:** transacoes com valor nao positivo sao rejeitadas, tanto na entrada manual (Bean Validation) quanto no registro via IA.
- **Endpoint de comando por texto:** facilita testar o raciocinio do assistente sem precisar gravar audio.
- **Tratamento de erros centralizado** com respostas HTTP adequadas.
- **Testes automatizados** para as regras de saldo e validacao.

## Estrutura do projeto

```
src/main/java/com/lucdev/orcamentoia/
├── OrcamentoIaApplication.java
├── config/
│   └── ChatClientConfig.java          # Configuracao do ChatClient e system prompt
├── controller/
│   ├── AssistenteController.java      # Endpoints de audio e texto
│   ├── TransacaoController.java       # Endpoints REST de transacoes
│   └── ErrosController.java           # Tratamento centralizado de erros
├── dto/
│   ├── ComandoResponse.java
│   ├── NovaTransacaoRequest.java
│   └── TransacaoResponse.java
├── model/
│   ├── Transacao.java
│   └── TipoTransacao.java
├── repository/
│   └── TransacaoRepository.java
├── service/
│   ├── AssistenteService.java         # Orquestra o ChatClient com as tools
│   ├── TranscricaoService.java        # Converte audio em texto
│   └── TransacaoService.java          # Regras de negocio das transacoes
└── tool/
    └── FinancasTools.java             # Funcoes expostas para o Tool Calling
```

## O que aprendi durante o desafio

- Como configurar o Spring AI e conectar a aplicacao a um modelo de linguagem.
- Como usar o `ChatClient` para conversar com o modelo e manter um system prompt de contexto.
- Como funciona o **Tool Calling**: o modelo escolhe e chama funcoes reais da aplicacao, conectando a IA a regras de negocio de verdade.
- Como transcrever audio em texto e encadear isso com o restante do fluxo.
- Como manter a separacao de responsabilidades (controllers, services, tools, repositorios) mesmo integrando recursos de IA.

## Autor

Lucas Montalvao — [github.com/lucasmontalvao1619](https://github.com/lucasmontalvao1619) — [lucdevv.vercel.app](https://lucdevv.vercel.app)
