# Fast Finance Helper — Assistente Financeiro de Voz

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

Sai um `dist/executavel/Fast Finance Helper.app` (~149 MB). Copie para onde preferir —
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

O executavel embute o **Java** e o **interpretador de comandos**, entao o
assistente escrito funciona de imediato, sem nada instalado e sem custo. O que
ele nao embute e a **IA**: para frases livres e para o ditado por voz, informe
uma chave da OpenAI em Configuracoes.

### Opcao 2 — Docker, sem instalar dependencia nenhuma

Para quem nao tem Java na maquina. O `docker-compose.yml` sobe apenas a
aplicacao — desde que a IA passou a ser da OpenAI, nao ha mais servico de modelo
local nem download de gigabytes.

```bash
scripts/iniciar.sh     # macOS e Linux
```

Na raiz tambem ha atalhos clicaveis que chamam esse script:

| Sistema | Iniciar | Parar |
|---------|---------|-------|
| macOS | **Iniciar Fast Finance Helper** | **Parar Fast Finance Helper** |
| Windows | **Iniciar Fast Finance Helper.bat** | `scripts/parar.sh` |

O script escolhe sozinho: usa o **modo local** quando a maquina tem Java, e cai
para o **Docker** quando nao tem. O local vem primeiro porque sobe em segundos e
nao depende de o Docker estar aberto.

Para encerrar, `scripts/parar.sh`, que serve aos dois modos.

> **Testado de ponta a ponta.** O `.deb` do Linux foi instalado e executado num
> Ubuntu 24.04 real (container), respondendo comandos do assistente. O `.exe` do
> Windows e verificado pelo GitHub Actions, que **executa o binario** numa
> maquina Windows antes de publicar.

### Opcao 3 — Local, sem Docker e sem empacotar

Requer **Java 17+** e **Maven**. E o caminho de quem vai mexer no codigo.

```bash
./mvnw spring-boot:run
```

Nao precisa de Ollama nem de chave: o interpretador proprio atende os comandos.
Quem quiser o modelo local mesmo assim:

```bash
brew install ollama && ollama serve && ollama pull qwen2.5:3b
AI_PROVIDER=ollama ./mvnw spring-boot:run
```

### Executaveis pelo GitHub Actions

| Sistema | O que sai | Como usar |
|---------|-----------|-----------|
| macOS | `Fast Finance Helper.app` | dois cliques |
| Windows | pasta com `Fast Finance Helper.exe` | dois cliques no .exe |
| Linux | `.deb` **e** pasta portatil | `sudo dpkg -i fast-finance-helper_1.0.0_amd64.deb` |

O `.deb` atende **Ubuntu, Debian e Zorin** — o Zorin e derivado do Ubuntu, que e
derivado do Debian. Para outras familias (Fedora, Arch), use a pasta portatil e
execute `bin/Fast Finance Helper`.


O `jpackage` so empacota para o sistema em que roda, entao o `.exe` do Windows
nao pode ser gerado a partir de um Mac. O workflow `.github/workflows/
executaveis.yml` resolve isso pedindo uma maquina de cada sistema ao GitHub.

Va em **Actions > Executaveis > Run workflow**, ou publique uma tag `v*`: os
executaveis dos tres sistemas saem prontos como artefatos, e numa tag ainda sao anexados a
Release. O workflow roda a suite de testes antes de empacotar e confere se o
binario foi realmente produzido, para nao publicar um zip vazio.

### No celular (Android e iOS)

A aplicacao e um **PWA**: da para instalar na tela inicial e usar como um app,
com icone proprio e sem a barra do navegador.

1. Suba a aplicacao no computador (o atalho **Iniciar Fast Finance Helper**). Ele
   imprime o endereco
   da maquina na rede local, algo como `http://192.168.0.10:8080`.
2. No celular, **na mesma rede Wi-Fi**, abra esse endereco no navegador.
3. Instale: no Android, menu do Chrome > *Instalar aplicativo*. No iOS, botao de
   compartilhar do Safari > *Adicionar a Tela de Inicio*.

O que isso e e o que nao e: o celular vira o **cliente**; quem processa continua
sendo o computador. O aparelho precisa estar na mesma rede e o computador
ligado, porque nenhum celular roda um modelo de linguagem de 1,9 GB. Para usar
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

Sao 16 ferramentas expostas ao modelo, e o interpretador proprio cobre todas:

| Area | Ferramentas |
|------|-------------|
| Transacoes | registrar, atualizar, apagar, listar |
| Consultas | saldo, gasto por categoria |
| Salario | definir, consultar, declarar que nao ha salario fixo |
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

## O assistente funciona sem chave e sem modelo baixado

O comando escrito e atendido por um **interpretador proprio**, em Java, que
entende as frases comuns por regras de texto — sem modelo de linguagem, sem
download e sem custo. Ele chama exatamente as mesmas ferramentas que a IA
chamaria, entao as regras de negocio e as respostas sao identicas nos dois
caminhos.

| Caminho | Quando | Tempo medido | Custo |
|---------|--------|--------------|-------|
| Interpretador proprio | padrao, sem chave | **33 a 113 ms** | zero |
| IA (OpenAI) | com chave configurada | 2 a 4 s | credito da conta |

Exemplos que o interpretador entende:

```
gastei 60 no uber                 recebi 500 de freela
meu salario e 3000, dia 15        qual e o meu saldo
quanto gastei com transporte      guarda 200 no porquinho
listar transacoes                 corrige a transacao 3 para 45
apaga a transacao 3               tira 100 do porquinho
```

Quando nao reconhece a frase, ele responde com esta lista em vez de um "nao
entendi" seco. Com chave configurada, a IA assume e cobre frases livres — e **se
a OpenAI recusar a chamada** (conta sem credito, por exemplo), o comando cai de
volta no interpretador em vez de falhar, avisando o que aconteceu.

## Voz: exige a chave da OpenAI

O ditado por voz funciona de duas formas, e a segunda e opcional:

| Caminho | Onde funciona | Custo |
|---------|---------------|-------|
| Navegador (padrao) | Chrome do computador e do Android | gratuito |
| Whisper no servidor | qualquer navegador, **inclusive o Safari** | credito da OpenAI |

O Safari nao tem reconhecimento de voz, entao no iPhone e no Mac o campo de
texto e a saida — a menos que exista uma chave configurada. Em
**Configuracoes > Voz** da para informar uma chave da OpenAI: a transcricao
passa a ser feita no servidor e vale **a partir da proxima chamada, sem
reiniciar** o aplicativo.

A chave fica guardada apenas nesta maquina, no banco local dentro da pasta
pessoal do usuario. A interface **nunca recebe a chave de volta** — so a
informacao de que existe uma configurada.

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

**O modelo padrao e o qwen2.5:3b, e a escolha foi medida.** O 7B (4,7 GB) leva
~18s por comando; o 1.5B (986 MB) responde em 1 a 4s mas **nao chama as
ferramentas** — pede descricao em vez de registrar, diz que precisa saber as
receitas em vez de consultar o saldo. Rapido e inutil. O 3B (1,9 GB) acertou
todas as chamadas testadas — registrar, corrigir, apagar, consultar saldo, gasto
por categoria, porquinho e salario — em **2 a 3 segundos**. Menos da metade do
download e cerca de seis vezes mais rapido, sem perder o Tool Calling. Para
trocar: `OLLAMA_MODEL=qwen2.5 ./mvnw spring-boot:run`.

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
| PUT | `/api/configuracao/chave-openai` | Guarda a chave da OpenAI (voz e IA) |
| DELETE | `/api/configuracao/chave-openai` | Remove a chave |
| GET | `/api/sessao` | Se o app esta em modo aplicativo |
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

Java 17 · Spring Boot 3.5 · Spring AI 1.1 · OpenAI (Ollama como alternativa) ·
Spring Data JPA · H2 · Bean Validation · SpringDoc · Docker ·
HTML, CSS e JavaScript puro · JUnit 5, Mockito e AssertJ

## Testes

```bash
./mvnw test
```

158 testes cobrindo saldo, validacao, configuracao de salario, rendimento do
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

Binario nao entra no Git: os atalhos do macOS e os executaveis sao **gerados**
por script. O repositorio guarda a receita, nao o produto — por isso ele tem
cerca de 1 MB, e nao os 370 MB dos aplicativos prontos.

```
├── Iniciar Fast Finance Helper.bat      # Atalho do Windows
│                                        # (os .app do macOS sao gerados por
│                                        #  scripts/gerar-atalhos.sh, nao versionados)
├── .github/workflows/            # Gera os executaveis de Windows e macOS
├── docker/                       # Dockerfile e docker-compose.yml
├── scripts/                      # iniciar, parar, empacotar e gerar executavel
│   ├── atalhos/                  # corpo dos atalhos do macOS
│   └── icone.icns / icone.ico    # fonte unica dos icones de empacotamento
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
