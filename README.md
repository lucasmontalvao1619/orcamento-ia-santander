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

Ha tres caminhos. O primeiro e o que da menos trabalho a quem so quer usar.

### Opcao 1 — Aplicativo de duplo clique (macOS, testado)

Um aplicativo com o **Java embutido**: nao exige Java, Maven nem Docker na
maquina. E o formato para mandar a aplicacao para outra pessoa.

```bash
scripts/gerar-executavel.sh
```

Sai um `dist/executavel/Orcamento IA.app` (~213 MB). Copie para onde preferir —
Desktop, Aplicativos — e de dois cliques. Como funciona no uso:

- **Nao abre Terminal.** A aplicacao sobe em segundo plano, o macOS avisa por
  notificacao e o navegador abre sozinho em http://localhost:8080.
- **Fechar a janela encerra o programa.** A pagina manda um sinal de vida a
  cada 4 segundos; quando os sinais param por mais de 15 segundos, a aplicacao
  se encerra. A folga existe para um F5 nao fechar o aplicativo.
- **Os lancamentos ficam em `~/.orcamento-ia`**, e nao dentro do aplicativo: um
  programa instalado nao pode gravar na propria pasta, e no macOS o diretorio
  de trabalho dele e a raiz do disco. Como e a pasta pessoal, cada conta do
  sistema tem o proprio orcamento, e ele sobrevive a fechar e reabrir.
- **Comeca zerado.** No primeiro acesso a aplicacao pergunta o salario; cada
  pessoa configura o seu.

Na **primeira vez** o macOS recusa aplicativos sem assinatura: clique com o
**botao direito > Abrir** e confirme. Depois o duplo clique funciona normalmente.

O **`.exe` do Windows** sai do mesmo empacotador pelo GitHub Actions, porque o
`jpackage` so gera para o sistema em que roda — nao ha como produzir um binario
Windows a partir de um Mac. Veja *Executaveis pelo GitHub Actions*, mais abaixo.

Uma ressalva honesta: o executavel embute o **Java**, nao a **IA**. O modelo tem
4,7 GB e continua vindo do Ollama ou do Docker. Sem ele a interface abre e os
lancamentos manuais funcionam; o assistente por voz responde erro.

### Opcao 2 — Docker, sem instalar dependencia nenhuma

Este e o caminho pensado para **nao exigir nada** alem do proprio Docker: nem
Java, nem Maven, nem Ollama, nem o download manual do modelo. O
`docker-compose.yml` sobe tres servicos — o modelo de linguagem, o download dele
e a aplicacao — e a aplicacao so inicia depois que o modelo termina de baixar,
para nunca subir com o assistente quebrado.

```bash
scripts/iniciar.sh     # macOS e Linux
```

Na raiz tambem ha atalhos clicaveis que chamam esse script:

| Sistema | Iniciar | Parar |
|---------|---------|-------|
| macOS | **Iniciar Orcamento IA** | **Parar Orcamento IA** |
| Windows | **Iniciar Orcamento IA.bat** | `scripts/parar.sh` |

O script escolhe sozinho como subir: usa o **Docker** quando ele esta
disponivel, e cai para o **modo local** quando nao esta, desde que a maquina
tenha Java e Ollama — inclusive iniciando o Ollama e baixando o modelo se
faltar. "O Docker nao esta rodando" e uma resposta inutil para quem tem todas as
pecas instaladas e so queria abrir o aplicativo.

A primeira execucao baixa o modelo (~4,7 GB) e leva alguns minutos; nas
seguintes a subida e rapida. Para encerrar, `scripts/parar.sh`, que serve aos
dois modos.

> **Estado dos testes:** o `docker-compose.yml` foi validado com
> `docker compose config` (contexto de build, volumes e caminhos), mas o
> `docker compose up` ainda **nao foi executado de ponta a ponta** — a maquina
> de desenvolvimento nao conseguiu subir o daemon. O caminho da Opcao 1 e o
> modo local, esses sim, foram testados rodando.

### Opcao 3 — Local, sem Docker e sem empacotar

Requer **Java 17+**, **Maven** e **Ollama**. E o caminho de quem vai mexer no
codigo.

```bash
brew install ollama        # macOS. Outras plataformas: https://ollama.com/download
ollama serve               # deixa o servidor rodando
ollama pull qwen2.5        # baixa o modelo (~4,7 GB, so na primeira vez)

./mvnw spring-boot:run
```

### Executaveis pelo GitHub Actions

O `jpackage` so empacota para o sistema em que roda, entao o `.exe` do Windows
nao pode ser gerado a partir de um Mac. O workflow `.github/workflows/
executaveis.yml` resolve isso pedindo uma maquina de cada sistema ao GitHub.

Va em **Actions > Executaveis > Run workflow**, ou publique uma tag `v*`: os
dois executaveis saem prontos como artefatos, e numa tag ainda sao anexados a
Release. O workflow roda a suite de testes antes de empacotar e confere se o
binario foi realmente produzido, para nao publicar um zip vazio.

### No celular (Android e iOS)

A aplicacao e um **PWA**: da para instalar na tela inicial e usar como um app,
com icone proprio e sem a barra do navegador.

1. Suba a aplicacao no computador (o atalho **Iniciar Orcamento IA**). Ele
   imprime o endereco
   da maquina na rede local, algo como `http://192.168.0.10:8080`.
2. No celular, **na mesma rede Wi-Fi**, abra esse endereco no navegador.
3. Instale: no Android, menu do Chrome > *Instalar aplicativo*. No iOS, botao de
   compartilhar do Safari > *Adicionar a Tela de Inicio*.

O que isso e e o que nao e: o celular vira o **cliente**; quem processa continua
sendo o computador. O aparelho precisa estar na mesma rede e o computador
ligado, porque nenhum celular roda um modelo de linguagem de 4,7 GB. Para usar
de qualquer lugar, a aplicacao precisaria estar hospedada num servidor.

O ditado por voz depende da Web Speech API: funciona no Chrome do Android e nao
funciona no Safari do iOS, onde o campo de texto continua disponivel.

### Gerar o zip para distribuir

```bash
scripts/empacotar.sh
```

Produz `dist/orcamento-ia.zip` sem build, sem dados e sem historico do git. Quem
receber extrai e da dois cliques no atalho — o orcamento comeca zerado.

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

Sao 15 ferramentas expostas ao modelo:

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

- **Onboarding** no primeiro acesso, perguntando o salario inicial — com a saida
  **"nao tenho salario fixo"** para renda variavel, autonomo ou quem ainda nao
  tem renda: o orcamento comeca zerado e cada entrada e registrada conforme o
  dinheiro chega.
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

**Nao ter salario e uma resposta valida.** Antes, "configurado" significava ter
um salario, entao quem vive de renda variavel so sairia da tela de boas-vindas
inventando um valor — que entraria no saldo como receita e mentiria sobre quanto
a pessoa tem. Declarar que nao tem salario libera o app com o orcamento zerado.
Voltar atras funciona nos dois sentidos, e ao declarar que nao tem salario a
receita antiga e apagada: mante-la deixaria no saldo um dinheiro que a pessoa
acabou de dizer que nao recebe.

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

**Fechar a janela encerra o aplicativo.** Como programa de duplo clique, o
usuario espera que o X feche o programa — so que a interface e uma pagina web, e
fechar a aba nao derrubaria o servidor. A pagina manda um sinal de vida
periodico; quando os sinais cessam, a aplicacao se encerra. A tolerancia de 15
segundos existe para o F5 nao fechar o aplicativo, ja que entre o unload e o
load os sinais param por um instante. Vale so no modo empacotado: num servidor,
encerrar por falta de navegador aberto seria o comportamento errado.

**O nome do projeto no Docker e fixo.** O `docker-compose.yml` declara
`name: orcamento-ia` em vez de deixar o Compose usar o nome da pasta. Sem isso,
mover ou renomear a pasta mudaria o nome do volume e os lancamentos sumiriam —
continuariam existindo, so que num volume que ninguem mais monta.

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
| PUT | `/api/configuracao/sem-salario` | Declara que nao ha salario fixo |
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

107 testes cobrindo saldo, validacao, configuracao de salario, rendimento do
porquinho, correcao e exclusao, as 15 ferramentas de Tool Calling, a transcricao
de audio, o encerramento do modo aplicativo e o contrato HTTP de todos os
endpoints — inclusive os status 400, 404,
502 e 503. Nao dependem de rede, de chave de API nem do Ollama.

Os testes usam um banco **em memoria**, configurado em
`src/test/resources/application.properties`. Sem esse arquivo eles herdariam a
configuracao de producao e escreveriam no banco em arquivo — ou seja, rodar a
suite mexeria nos lancamentos reais de quem estivesse usando o aplicativo.

---

## Estrutura

```
├── Iniciar Orcamento IA.app      # Atalho do macOS: duplo clique e pronto
├── Parar Orcamento IA.app        # Encerra sem precisar de Terminal
├── Iniciar Orcamento IA.bat      # O mesmo atalho no Windows
├── .github/workflows/            # Gera os executaveis de Windows e macOS
├── docker/                       # Dockerfile e docker-compose.yml
├── scripts/                      # iniciar, parar, empacotar e gerar executavel
├── src/
│   ├── main/
│   │   ├── java/com/lucdev/orcamentoia/
│   │   │   ├── config/           # ChatClient, system prompt e autoria
│   │   │   ├── controller/       # Assistente, transacoes, investimentos, configuracao
│   │   │   ├── dto/
│   │   │   ├── exception/        # Erros centralizados em ProblemDetail
│   │   │   ├── model/
│   │   │   ├── repository/
│   │   │   ├── service/          # Regras de negocio
│   │   │   └── tool/             # Funcoes expostas ao Tool Calling
│   │   └── resources/static/     # Interface web (sem build step)
│   └── test/                     # Testes espelhando a estrutura acima
└── dados/                        # Banco H2 local (nao versionado)
```

A raiz guarda so o que precisa estar a vista: o atalho para iniciar, o README e
os arquivos do Maven. O resto e detalhe de execucao e mora em `docker/` e
`scripts/`.

## Autor

**Lucas Montalvão** — [github.com/lucasmontalvao1619](https://github.com/lucasmontalvao1619) — [lucdevv.vercel.app](https://lucdevv.vercel.app)
