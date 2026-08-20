# Orçamento IA — Assistente Financeiro por Voz

Controle de financas pessoais construido com **Spring Boot** e **Spring AI**, com
interface web propria. O usuario fala ou digita um comando em linguagem natural
("gastei 60 no uber"), a aplicacao interpreta a intencao com um modelo de
linguagem e executa a acao real correspondente: registrar, corrigir, apagar ou
consultar lancamentos.

O modelo roda **localmente**, sem chave de API e sem custo. A troca para a OpenAI
e uma variavel de ambiente.

---

## Como executar

### Opcao 1 — Docker (recomendado)

Requer apenas o **Docker** instalado e aberto. Nao precisa de Java, Maven nem Ollama.

```bash
./iniciar.sh          # macOS e Linux
iniciar.bat           # Windows
```

O script sobe tres servicos: o modelo de IA, o download do modelo e a aplicacao.
Na primeira execucao o modelo e baixado (~4,7 GB) e isso leva alguns minutos; nas
seguintes ele ja esta no volume e a subida e rapida.

Depois abra **http://localhost:8080**. Para encerrar, `./parar.sh`.

Cada instalacao comeca com o **orcamento zerado**: o volume de dados nasce vazio,
e a aplicacao pergunta o salario no primeiro acesso.

### Opcao 2 — Local, sem Docker

Requer **Java 17**, **Maven** e **Ollama**.

```bash
brew install ollama        # macOS. Outras plataformas: https://ollama.com/download
ollama serve               # deixa o servidor rodando
ollama pull qwen2.5        # baixa o modelo (~4,7 GB, so na primeira vez)

./mvnw spring-boot:run
```

### Usando a OpenAI no lugar do modelo local

```bash
export OPENAI_API_KEY="sua-chave-aqui"
AI_PROVIDER=openai ./mvnw spring-boot:run
```

A chave sai de `platform.openai.com` e exige credito na conta — uma chave sem
credito e criada normalmente mas falha em toda chamada com `insufficient_quota`.

A **transcricao de audio no servidor** (Whisper) so existe na OpenAI e depende de
`OPENAI_API_KEY` mesmo com o modelo local. Sem ela, o ditado por voz e feito pelo
proprio navegador, que nao consome credito.

---

## O que o assistente faz

O modelo nao escreve no banco. Ele escolhe **qual funcao Java chamar e com quais
argumentos** — isso e o **Tool Calling**. Quem executa e a aplicacao, passando
pelas mesmas regras de negocio da API REST.

Sao 13 ferramentas expostas ao modelo:

| Area | Ferramentas |
|------|-------------|
| Transacoes | registrar, atualizar, apagar, listar |
| Consultas | saldo, gasto por categoria |
| Salario | definir, consultar |
| Porquinho | guardar, retirar, consultar, listar movimentos, apagar movimento |
| App | recursos disponiveis, autor do projeto |

Exemplos que funcionam:

```
"Gastei 60 reais no uber"                 -> despesa em transporte (categoria inferida)
"Recebi 500 de presente da minha avo"     -> receita em presente
"Meu salario e 3000, todo dia 15"         -> configura o salario, sem somar duas vezes
"Guarda 800 no porquinho para a viagem"   -> aporte no porquinho
"Corrige a transacao 3 para 60 reais"     -> atualiza o lancamento
"Quanto eu tenho guardado?"               -> total com rendimento
"O que esse app faz?"                     -> lista os recursos reais
```

---

## Recursos

- **Onboarding** no primeiro acesso, perguntando o salario inicial.
- **Lancamentos** por voz, texto ou formulario, com correcao e exclusao.
- **Cinco categorias fixas de despesa** (alimentacao, transporte, moradia, lazer,
  saude) e tres de receita (salario, presente, extra).
- **Salario configuravel**, com o dia do mes em que cai.
- **Porquinho de investimento**, separado do saldo, rendendo **100% do CDI**.
- **Interface web** sem build step, servida pelo proprio Spring.

O ditado de voz usa a **Web Speech API** do navegador (Chrome), mantendo o fluxo
de voz sem consumir credito de transcricao.

---

## Decisoes de implementacao

**O salario nao duplica no saldo.** A configuracao guarda o id da receita de
salario; alterar o valor atualiza aquela transacao em vez de criar outra.

**O porquinho e um controle a parte.** Guardar dinheiro nao reduz o saldo do
orcamento: sao dois totais paralelos. O rendimento e calculado percorrendo os
movimentos em ordem e corrigindo o saldo entre um e outro, entao o que rendeu
tambem rende. Dias uteis (252/ano); feriados sao ignorados de proposito.

**A validacao vale nos dois caminhos.** A entrada REST e a entrada da IA caem no
mesmo metodo do service, entao a regra de valor positivo nao depende do `@Valid`
do controller.

**A aplicacao sobe sem chave de API.** As propriedades tem valor default, e a
interface consulta `/api/assistente/status` para avisar antes de o usuario tentar
um comando que so resultaria em erro.

**A memoria de conversa fica desligada por padrao.** Ela faz "na verdade foram 60"
funcionar, mas modelos locais menores, ao verem no historico respostas antigas no
formato "registrado com sucesso", passam a **imitar esse texto em vez de chamar a
ferramenta** — respondem que atualizaram sem que nada mude no banco. Foi medido:
sem historico a ferramenta e chamada e o valor muda; com historico o modelo apenas
descreve o que faria. Um erro silencioso desses e pior que a falta do recurso.
Com um modelo maior, ligue com `ASSISTENTE_MEMORIA=true`.

**A autoria tem fonte unica no backend** (`config/Autoria`) e chega a interface
por `/api/sobre`, alem de alimentar a ferramenta que responde quem fez o projeto.

---

## Endpoints

| Metodo | Rota | Descricao |
|--------|------|-----------|
| GET | `/api/assistente/status` | Se o assistente esta utilizavel e qual o provedor |
| POST | `/api/assistente/texto` | Processa um comando de texto |
| POST | `/api/assistente/audio` | Processa um comando de voz (requer OpenAI) |
| GET | `/api/configuracao` | Estado da configuracao inicial e salario |
| PUT | `/api/configuracao/salario` | Define ou altera o salario e o dia |
| GET | `/api/categorias` | Categorias de despesa e de receita |
| POST | `/api/transacoes` | Cria uma transacao |
| PUT | `/api/transacoes/{id}` | Corrige uma transacao (campos opcionais) |
| DELETE | `/api/transacoes/{id}` | Apaga uma transacao |
| GET | `/api/transacoes` | Lista transacoes (filtro opcional por `tipo`) |
| GET | `/api/transacoes/saldo` | Saldo atual |
| POST | `/api/investimentos` | Registra aporte ou retirada |
| DELETE | `/api/investimentos/{id}` | Apaga um movimento |
| GET | `/api/investimentos` | Lista os movimentos |
| GET | `/api/investimentos/resumo` | Total, rendimento e taxa |
| GET | `/api/sobre` | Autoria do projeto |

Documentacao interativa em `/swagger-ui.html`.

---

## Contrato de erros

Todas as falhas saem em [ProblemDetail (RFC 7807)](https://datatracker.ietf.org/doc/html/rfc7807).

| Situacao | Status |
|----------|--------|
| Corpo invalido ou valor nao positivo | `400 Bad Request` |
| Id inexistente | `404 Not Found` |
| Provedor de IA recusou a chamada | `502 Bad Gateway` |
| Provedor de IA indisponivel | `503 Service Unavailable` |
| Modelo local fora do ar | `503 Service Unavailable` |

---

## Tecnologias

Java 17 · Spring Boot 3.5 · Spring AI 1.1 · Ollama (OpenAI como alternativa) ·
Spring Data JPA · H2 · Bean Validation · SpringDoc · Docker ·
HTML, CSS e JavaScript puro · JUnit 5, Mockito e AssertJ

## Testes

```bash
./mvnw test
```

Cobrem saldo, validacao, configuracao de salario, rendimento do porquinho,
correcao e exclusao, as ferramentas de Tool Calling e o contrato HTTP. Nao
dependem de rede, de chave de API nem do Ollama.

---

## Estrutura

```
├── Dockerfile / docker-compose.yml     # Execucao em container
├── iniciar.sh / parar.sh / iniciar.bat # Atalhos de uso
└── src/main/
    ├── java/com/lucdev/orcamentoia/
    │   ├── config/       # ChatClient, system prompt e autoria
    │   ├── controller/   # Assistente, transacoes, investimentos, configuracao
    │   ├── dto/
    │   ├── exception/    # Erros centralizados em ProblemDetail
    │   ├── model/
    │   ├── repository/
    │   ├── service/      # Regras de negocio
    │   └── tool/         # Funcoes expostas ao Tool Calling
    └── resources/static/ # Interface web (sem build step)
```

## Autor

**Lucas Montalvão** — [github.com/lucasmontalvao1619](https://github.com/lucasmontalvao1619) — [lucdevv.vercel.app](https://lucdevv.vercel.app)
