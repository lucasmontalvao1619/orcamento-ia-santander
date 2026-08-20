# Orcamento IA - Assistente Financeiro por Voz

Aplicacao de controle de financas pessoais construida com **Spring Boot** e **Spring AI**,
com interface web propria. O usuario fala ou digita um comando em linguagem natural
("gastei 50 reais com almoco"), a aplicacao interpreta a intencao com um modelo de
linguagem e executa a acao real correspondente: registrar uma receita/despesa ou
consultar saldo, gastos e transacoes.

O modelo roda **localmente via Ollama**, sem chave de API e sem custo. A troca para
a OpenAI e uma variavel de ambiente.

## O que o projeto faz

1. O usuario fala (ou digita) um comando na interface web.
2. O audio vira texto — no navegador (Web Speech API) ou no servidor (Whisper).
3. O texto vai ao modelo de linguagem atraves do `ChatClient`.
4. O modelo decide, via **Tool Calling**, qual funcao da aplicacao executar.
5. A funcao registra ou consulta transacoes no banco.
6. Uma resposta em linguagem natural volta para a tela.

Alem do assistente, a aplicacao funciona por completo sem IA nenhuma: lancamentos
manuais, saldo, filtros, categorias e configuracao de salario.

## Tecnologias utilizadas

- Java 17
- Spring Boot 3.5
- Spring AI 1.1 (ChatClient, Tool Calling, transcricao de audio)
- Ollama (modelo local) com OpenAI como provedor alternativo
- Spring Data JPA + banco H2 (em memoria)
- Bean Validation
- SpringDoc / Swagger UI
- Frontend em HTML, CSS e JavaScript puro (sem build step)
- JUnit 5, Mockito e AssertJ

## Como executar

Pre-requisitos: **Java 17**, **Maven** e **Ollama**.

### 1. Instalar e preparar o Ollama

```bash
brew install ollama        # macOS. Outras plataformas: https://ollama.com/download
ollama serve               # deixa o servidor rodando
ollama pull llama3.1       # baixa o modelo (~5 GB, so na primeira vez)
```

### 2. Subir a aplicacao

```bash
./mvnw spring-boot:run
```

Abra **http://localhost:8080**. No primeiro acesso a aplicacao pergunta o seu
salario (ou quanto voce tem disponivel) e usa esse valor como receita inicial.

### Usando a OpenAI no lugar do Ollama

O provedor e selecionavel sem alterar codigo:

```bash
export OPENAI_API_KEY="sua-chave-aqui"
AI_PROVIDER=openai ./mvnw spring-boot:run
```

A chave sai de `platform.openai.com` e exige credito na conta — uma chave sem
credito e criada normalmente mas falha em toda chamada com `insufficient_quota`.

A **transcricao de audio no servidor** (Whisper) existe apenas na OpenAI e depende
de `OPENAI_API_KEY` mesmo com o Ollama como provedor de chat. Sem ela, o ditado por
voz e feito pelo proprio navegador, que nao consome credito nenhum.

## Interface web

A interface e servida pelo proprio Spring a partir de `src/main/resources/static/`
— sem npm, sem build step. Ela oferece:

- **Tela de boas-vindas** no primeiro acesso, pedindo o salario inicial.
- **Cartoes de resumo** com saldo, total de receitas e total de despesas.
- **Assistente** com botao de microfone e campo de texto, mais comandos sugeridos.
- **Lancamento manual** com cinco categorias fixas de despesa e atalhos para
  registrar presente ou renda extra.
- **Configuracoes** para alterar o salario a qualquer momento.
- **Tabela de transacoes** com filtro por tipo.

O ditado de voz usa a **Web Speech API** do navegador (Chrome), o que mantem o
fluxo de voz funcionando sem consumir credito de transcricao.

## Categorias

Fixas, expostas pelo endpoint `/api/categorias` para que a interface nao mantenha
uma copia propria da lista:

| Despesas | Receitas |
|----------|----------|
| Alimentacao, Transporte, Moradia, Lazer, Saude | Salario, Presente, Renda extra |

O campo `categoria` da transacao continua sendo texto livre, porque o assistente
pode criar categorias novas ao registrar por voz.

## Endpoints

| Metodo | Rota | Descricao |
|--------|------|-----------|
| GET | `/api/assistente/status` | Diz se o assistente esta configurado e qual o provedor |
| POST | `/api/assistente/texto` | Processa um comando de texto |
| POST | `/api/assistente/audio` | Processa um comando de voz (requer OpenAI) |
| GET | `/api/configuracao` | Estado da configuracao inicial e salario atual |
| PUT | `/api/configuracao/salario` | Define ou altera o salario |
| GET | `/api/categorias` | Lista as categorias de despesa e de receita |
| POST | `/api/transacoes` | Cria uma transacao manualmente |
| GET | `/api/transacoes` | Lista transacoes (filtro opcional por `tipo`) |
| GET | `/api/transacoes/saldo` | Retorna o saldo atual |

## Decisoes de implementacao

- **O salario nao duplica no saldo.** A configuracao guarda o id da receita de
  salario; alterar o valor atualiza aquela transacao em vez de criar outra.
- **A validacao vale nos dois caminhos.** A entrada REST e a entrada da IA caem
  no mesmo metodo do service, entao a regra de valor positivo nao depende do
  `@Valid` do controller.
- **A aplicacao sobe sem chave de API.** As propriedades tem valor default, e a
  interface consulta `/api/assistente/status` para avisar antes de o usuario
  tentar um comando que so resultaria em erro.
- **Erros padronizados em ProblemDetail (RFC 7807),** incluindo as falhas do
  provedor de IA e a indisponibilidade do modelo local.
- **Locale fixo (pt-BR)** na formatacao monetaria das respostas do assistente,
  para o texto nao mudar conforme a maquina que roda a aplicacao.

## Contrato de erros

| Situacao | Status |
|----------|--------|
| Corpo invalido (Bean Validation) ou valor nao positivo | `400 Bad Request` |
| Provedor de IA recusou a chamada (ex.: chave invalida) | `502 Bad Gateway` |
| Provedor de IA temporariamente indisponivel | `503 Service Unavailable` |
| Ollama nao esta rodando | `503 Service Unavailable` |

```json
{
  "type": "about:blank",
  "title": "Modelo local indisponivel",
  "status": 503,
  "detail": "Nao foi possivel conectar ao modelo local. Verifique se o Ollama esta rodando (ollama serve).",
  "instance": "/api/assistente/texto"
}
```

## Testes

```bash
./mvnw test
```

22 testes cobrindo saldo, validacao, configuracao de salario, as ferramentas de
Tool Calling e o contrato HTTP. Os testes de service e de tools usam Mockito e
nao dependem de rede, de chave de API nem do Ollama.

## Estrutura do projeto

```
src/main/java/com/lucdev/orcamentoia/
├── OrcamentoIaApplication.java
├── config/
│   └── ChatClientConfig.java          # ChatClient e system prompt
├── controller/
│   ├── AssistenteController.java      # Comandos de voz, texto e status
│   ├── ConfiguracaoController.java    # Salario e categorias
│   └── TransacaoController.java       # Endpoints REST de transacoes
├── exception/
│   └── ApiExceptionHandler.java       # Erros centralizados em ProblemDetail
├── dto/
├── model/
│   ├── CategoriaDespesa.java          # As cinco categorias fixas
│   ├── CategoriaReceita.java
│   ├── Configuracao.java              # Salario configurado
│   ├── TipoTransacao.java
│   └── Transacao.java
├── repository/
├── service/
│   ├── AssistenteService.java         # Orquestra o ChatClient com as tools
│   ├── ConfiguracaoService.java       # Regras do salario
│   ├── TranscricaoService.java        # Converte audio em texto
│   └── TransacaoService.java          # Regras das transacoes
└── tool/
    └── FinancasTools.java             # Funcoes expostas para o Tool Calling

src/main/resources/static/             # Interface web (sem build step)
├── index.html
├── css/estilo.css
└── js/app.js
```

## Autor

Lucas Montalvao — [github.com/lucasmontalvao1619](https://github.com/lucasmontalvao1619) — [lucdevv.vercel.app](https://lucdevv.vercel.app)
