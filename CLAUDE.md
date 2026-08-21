# Fast Finance Helper — instrucoes do projeto

Aplicativo de financas pessoais que roda na maquina do usuario: servidor Spring
Boot, interface web e assistente que executa acoes reais. Distribuido como
executavel de duplo clique para macOS, Windows e Linux.

---

## Regra 1 — o assistente precisa alcancar TODA funcao do software

Se um recurso existe na interface mas o assistente nao consegue executa-lo, o
recurso esta pela metade. Ja aconteceu duas vezes neste projeto:

- "nao tenho salario fixo" existia so como botao na tela. Nem a IA alcancava,
  porque nao havia ferramenta.
- O interpretador ficou sem cobrir recursos que a IA ja alcancava.

**Ao adicionar qualquer capacidade nova** — acoes da bolsa, percentual do CDI,
metas de gasto, categorias novas, o que for:

1. Crie a ferramenta em `tool/`, anotada com `@Tool`, para a IA chamar.
2. Adicione o reconhecimento em `service/InterpretadorDeComandos.java`, com as
   palavras que uma pessoa usaria de verdade.
3. Escreva testes: um por variacao de linguagem esperada.
4. Atualize `AppTools.consultarRecursosDoApp()` se a lista de recursos mudou —
   e o que o assistente responde quando perguntam o que o app faz.
5. Atualize o README.

Esquecer o passo 2 **nao quebra nenhum teste**: o recurso simplesmente nao
funciona para quem nao tem chave, em silencio.

### Excecao deliberada

Configurar a chave da OpenAI **nao** e ferramenta. O modelo nao deve poder
alterar as proprias credenciais.

---

## Regra 2 — o repertorio do interpretador cresce sempre

O `InterpretadorDeComandos` e o padrao, porque a maioria nao vai configurar
chave nenhuma. Ele deve ser ampliado continuamente: mais sinonimos, mais formas
de perguntar, mais palavras por categoria. Isso nao pesa no executavel — sao
strings e regex.

Ao mexer nele, revise tambem o que ja existe: uma pessoa pede a mesma coisa de
muitas formas ("qual meu saldo", "como estou", "posso gastar", "quanto sobrou").

### Casar PALAVRA INTEIRA, nunca pedaco

Categorias e saudacoes usam limite de palavra (`\b`). Comparar por substring
parece funcionar e falha em silencio: `gas` (moradia) casava dentro de
`gastos`, e "gastos com farmacia" virava moradia. Palavras curtas colidem com
maiores o tempo todo — `bar` em "barato", `moto` em "motorista".

### Frases de continuacao

O interpretador guarda a ultima acao para "mais 300" funcionar depois de
"guarde 500" — que e um comando completo na cabeca de quem escreve. Frase com
verbo ou categoria proprios NAO e continuacao: "gastei mais 300 no mercado"
precisa virar um gasto de alimentacao, e nao repetir o anterior.

### Verbo sozinho basta

Nao exija a palavra do dominio junto do verbo. "guarde 500" nao entendia porque
a regra pedia a palavra "porquinho" na frase — e essa e a forma mais natural de
pedir. O mesmo vale para "tira 100".

### Ordem das regras importa

Em `interpretar()`, as verificacoes vao do mais especifico para o mais generico.
"apagar movimento do porquinho" precisa vir antes de "apagar transacao", senao
um aporte apagaria um lancamento do orcamento. Frases sociais vem antes de tudo,
para um "oi" nao virar comando de dinheiro por conter algum numero.

---

## Decisoes que nao devem ser revertidas sem motivo

**A IA e opcional, nunca requisito.** O app funciona inteiro sem chave e sem
modelo baixado. Com chave da OpenAI, a IA assume e cobre frases livres; se ela
recusar (conta sem credito), o comando cai de volta no interpretador avisando o
motivo real.

**Nao voltar o Ollama como padrao.** Exigia 1,9 GB e um segundo programa
rodando — desproporcional. Continua acessivel via `AI_PROVIDER=ollama`.

**A voz depende da chave da OpenAI.** O reconhecimento do navegador cobre o
Chrome; o Safari nao tem esse recurso, e Whisper e o unico caminho universal.

**A chave nunca volta para a interface.** `ConfiguracaoResponse` expoe apenas
`chaveOpenAiConfigurada`, um booleano.

**Os dados ficam fora do aplicativo:** `~/.orcamento-ia` no executavel
empacotado, `./dados` no uso normal. Programa instalado nao grava na propria
pasta — no macOS o diretorio de trabalho dele e a raiz do disco.

**A porta nao e fixa.** A aplicacao procura a primeira livre a partir da 8080;
scripts e atalhos descobrem onde ela subiu. A 8080 e das mais disputadas, e
ocupada a aplicacao morria no boot.

**So encerrar apos ter recebido sinal de vida.** Uma pagina antiga em cache nao
envia sinal: a regra anterior matava o app com o usuario na frente dele.

**Binario nao entra no Git.** Atalhos saem de `scripts/gerar-atalhos.sh`,
executaveis do `jpackage`. O repositorio guarda a receita, nao o produto.

**O service worker NAO guarda a interface em cache.** Ele existe so para o app
poder ser instalado na tela inicial do celular, e guarda apenas icones e
manifest.

A regra anterior era "trocar a versao do cache a cada mudanca na interface".
Ela falhou duas vezes, das duas por esquecimento: uma mensagem de erro ja
removida do codigo reapareceu na tela, e abas novas nao apareciam com o servidor
ja servindo o HTML novo. Uma regra que depende de lembrar nao e uma regra.

E o cache da interface nunca teve valor aqui: a aplicacao so funciona com o
servidor rodando, entao uma interface guardada abriria uma tela que nao faz
nada. Nao adicione cache de HTML, JS ou CSS.

---

## Como trabalhar aqui

**Verificar rodando, nao so compilando.** Os defeitos mais caros deste projeto
passaram por toda a suite: porta ocupada, app se encerrando sozinho, interface
travando o envio, categoria errada por substring. Todos apareceram executando.

**Testes nao dependem de rede, chave ou modelo.** Banco em memoria, provedores
dublados. Se um teste precisar de rede, algo esta errado no desenho.

**Comentar o PORQUE, nao o que.** O codigo ja diz o que faz. Os comentarios
deste projeto registram a razao e, quando existe, o defeito que motivou a linha.

```bash
./mvnw test                    # 150+ testes, sem rede
scripts/gerar-executavel.sh    # executavel do sistema atual
scripts/gerar-atalhos.sh       # atalhos do macOS
```

O `.github/workflows/testes.yml` roda a suite a cada push. O
`executaveis.yml` gera os tres sistemas, **executa cada binario** e so entao
publica — conferir que o arquivo existe nao prova que ele abre.

---

## Fora de escopo, decidido

**Celular nao instala nada.** E cliente da aplicacao rodando no computador, via
PWA. O usuario esta desenvolvendo um app proprio para celular a parte.

**Sem loja de aplicativos.** A arquitetura (servidor pessoal) nao cabe nas
regras da App Store, e a Play Store hospedaria uma casca inutil sem o
computador ligado.

**Sem assinatura digital.** Exige certificado pago (US$ 99/ano na Apple). Os
avisos de origem desconhecida estao documentados no Leia-me de cada pacote.
