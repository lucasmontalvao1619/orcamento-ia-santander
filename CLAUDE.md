# Fast Finance Helper — instrucoes do projeto

## Regra principal: todo recurso novo precisa existir nos DOIS caminhos

O assistente tem dois motores, e eles nao sao alternativas equivalentes:

| Caminho | Quando e usado | Custo |
|---------|----------------|-------|
| `InterpretadorDeComandos` | sem chave da OpenAI configurada | zero, offline |
| IA (OpenAI, via Tool Calling) | quando ha chave configurada | credito da conta |

**O interpretador e o padrao**, porque a maioria das pessoas nao vai configurar
chave nenhuma. Um recurso que so a IA alcanca fica invisivel para elas.

Entao, ao adicionar qualquer capacidade nova ao aplicativo — por exemplo acoes
da bolsa, mudanca do percentual do CDI, metas de gasto, categorias novas:

1. Crie a ferramenta (`tool/`) como hoje, para a IA poder chama-la.
2. **Adicione o reconhecimento correspondente em
   `service/InterpretadorDeComandos.java`**, com as palavras que uma pessoa
   usaria de verdade para pedir aquilo.
3. **Escreva o teste em `InterpretadorDeComandosTest`**, com pelo menos uma
   frase por variacao de linguagem que voce espera atender.
4. Se o recurso mudar o texto de `AppTools.consultarRecursosDoApp()`, atualize
   la tambem — e o que o assistente responde quando perguntam o que o app faz.

Esquecer o passo 2 nao quebra nenhum teste existente: o recurso simplesmente
nao funciona para quem nao tem chave, em silencio. Por isso a regra esta aqui.

### Ordem das regras importa

Em `interpretar()`, as verificacoes vao do mais especifico para o mais generico.
"apagar movimento do porquinho" precisa ser testado antes de "apagar transacao",
senao um aporte apagaria um lancamento do orcamento. Ao inserir uma regra nova,
posicione-a considerando o que ja existe, e cubra o conflito com teste.

## Decisoes que nao devem ser revertidas sem motivo

**Nao voltar o Ollama como padrao.** Exigia 1,9 GB de download e um segundo
programa rodando, desproporcional para este aplicativo. Continua disponivel via
`AI_PROVIDER=ollama` para quem quiser.

**A voz depende da chave da OpenAI.** O reconhecimento do navegador cobre o
Chrome, mas o Safari nao tem esse recurso; a transcricao no servidor (Whisper) e
o unico caminho universal, e ela e paga.

**A chave nunca volta para a interface.** O `ConfiguracaoResponse` expoe apenas
`chaveOpenAiConfigurada`, um booleano.

**Os dados ficam fora do aplicativo.** No executavel empacotado, em
`~/.orcamento-ia`; no uso normal, em `./dados`. Um programa instalado nao pode
gravar na propria pasta.

**Binario nao entra no Git.** Os atalhos do macOS saem de
`scripts/gerar-atalhos.sh` e os executaveis do `jpackage`. O repositorio guarda
a receita, nao o produto.

## Verificacao

```bash
./mvnw test                    # suite completa, sem rede e sem chave
scripts/gerar-executavel.sh    # executavel do sistema atual
```

Ao mexer no assistente, teste os dois caminhos: com chave configurada e sem.
