package com.lucdev.orcamentoia.service;

import com.lucdev.orcamentoia.model.TipoTransacao;
import com.lucdev.orcamentoia.tool.AppTools;
import com.lucdev.orcamentoia.tool.FinancasTools;
import com.lucdev.orcamentoia.tool.FixosTools;
import com.lucdev.orcamentoia.tool.InvestimentoTools;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Entende os comandos escritos SEM modelo de linguagem nenhum.
//
// Existe para o aplicativo funcionar de graca e offline: sem isto, escrever
// "gastei 60 no uber" exigiria o Ollama (1,9 GB baixados) ou uma chave da
// OpenAI com credito. Com o interpretador, o app cumpre sua funcao sozinho, e a
// IA vira melhoria opcional para frases mais livres.
//
// Ele chama exatamente as mesmas ferramentas que a IA chamaria, entao as regras
// de negocio e as respostas sao as mesmas nos dois caminhos.
//
// IMPORTANTE: todo recurso novo do aplicativo precisa ser coberto aqui tambem.
// Um recurso que so a IA alcanca fica invisivel para quem nao tem chave.
@Service
public class InterpretadorDeComandos {

    // Numero em pt-BR ou en-US: 60, 60,50, 1.200,00, 60.50
    private static final Pattern VALOR = Pattern.compile("(\\d{1,3}(?:\\.\\d{3})*,\\d{2}|\\d+,\\d{1,2}|\\d+\\.\\d{1,2}|\\d+)");
    private static final Pattern ID = Pattern.compile("\\b(\\d{1,6})\\b");

    // Palavra -> categoria. A ordem importa: a primeira que casar vence.
    // Palavra -> categoria de DESPESA. A ordem importa: a primeira que casar
    // vence, entao termos ambiguos ficam depois dos especificos.
    private static final Map<String, String> CATEGORIAS_DESPESA = new LinkedHashMap<>();

    // Palavra -> categoria de RECEITA. Separado das despesas de proposito:
    // "presente" e receita, mas cairia em lazer se as listas fossem uma so.
    private static final Map<String, String> CATEGORIAS_RECEITA = new LinkedHashMap<>();

    private static void mapear(Map<String, String> destino, String categoria, String... palavras) {
        for (String palavra : palavras) {
            destino.put(palavra, categoria);
        }
    }

    static {
        mapear(CATEGORIAS_DESPESA, "transporte",
                "uber", "99pop", "99 ", "taxi", "onibus", "metro", "trem", "gasolina", "combustivel",
                "etanol", "alcool", "diesel", "passagem", "estacionamento", "pedagio", "corrida",
                "bilhete", "moto", "patinete", "bicicleta", "mecanico", "ipva", "licenciamento",
                "oficina", "pneu", "transporte");
        mapear(CATEGORIAS_DESPESA, "alimentacao",
                "almoco", "jantar", "janta", "lanche", "cafe", "mercado", "supermercado", "padaria",
                "restaurante", "ifood", "rappi", "pizza", "hamburguer", "burguer", "sushi", "comida",
                "feira", "acougue", "hortifruti", "sorvete", "doce", "delivery", "marmita", "cantina",
                "refeicao", "alimentacao");
        mapear(CATEGORIAS_DESPESA, "moradia",
                "aluguel", "condominio", "luz", "energia", "agua", "internet", "wifi", "gas",
                "faxina", "diarista", "iptu", "reforma", "movel", "eletrodomestico", "conta de casa",
                "moradia");
        mapear(CATEGORIAS_DESPESA, "lazer",
                "cinema", "bar", "balada", "show", "viagem", "jogo", "game", "netflix", "spotify",
                "disney", "hbo", "streaming", "festa", "teatro", "parque", "livro", "hobby",
                "assinatura", "lazer");
        mapear(CATEGORIAS_DESPESA, "saude",
                "farmacia", "remedio", "medico", "dentista", "exame", "consulta", "academia",
                "plano de saude", "terapia", "psicologo", "oculos", "hospital", "vacina", "saude");

        mapear(CATEGORIAS_RECEITA, "salario", "salario", "pagamento do mes", "holerite");
        mapear(CATEGORIAS_RECEITA, "presente",
                "presente", "aniversario", "natal", "mesada", "doacao");
        mapear(CATEGORIAS_RECEITA, "extra",
                "freela", "freelance", "bico", "extra", "venda", "vendi", "comissao", "premio",
                "reembolso", "restituicao", "rendimento");

        PADROES_DESPESA = compilar(CATEGORIAS_DESPESA);
        PADROES_RECEITA = compilar(CATEGORIAS_RECEITA);
    }

    private static final Map<Pattern, String> PADROES_DESPESA;
    private static final Map<Pattern, String> PADROES_RECEITA;

    // Frases sociais nao mexem em dinheiro, mas ignora-las faz o assistente
    // parecer quebrado logo no primeiro contato — quase todo mundo comeca com
    // um "oi".
    private static final String[] SAUDACOES = {"ola", "oi ", "oi", "bom dia", "boa tarde",
            "boa noite", "e ai", "eai", "tudo bem", "opa", "hey", "hello"};
    private static final String[] AGRADECIMENTOS = {"obrigado", "obrigada", "valeu", "vlw",
            "brigado", "agradeco"};
    private static final String[] DESPEDIDAS = {"tchau", "ate mais", "ate logo", "falou", "adeus"};
    private static final String[] PEDIDOS_DE_AJUDA = {"ajuda", "help", "comandos", "como funciona",
            "o que voce faz", "o que da pra fazer", "me ajuda", "socorro", "nao sei usar",
            "como usar", "exemplos"};

    // Uma frase por linha, sem colunas. A versao anterior alinhava exemplos em
    // duas colunas com espacos: em fonte proporcional, que e a da interface,
    // isso vira uma bagunca de texto desalinhado.
    static final String AJUDA = """
            Posso cuidar do seu orcamento por comandos escritos. Alguns exemplos:

            LANCAR
            • gastei 60 no uber
            • recebi 500 de freela
            • paguei 1.250,90 de aluguel
            • corrige a transacao 3 para 45
            • apaga a transacao 3

            CONSULTAR
            • qual e o meu saldo
            • quanto gastei com alimentacao
            • listar transacoes
            • para onde meu dinheiro esta indo

            SALARIO
            • meu salario e 3000, dia 15
            • nao tenho salario fixo

            CONTAS FIXAS
            • todo mes pago 89 de internet
            • o que falta pagar esse mes
            • fechar o mes

            PORQUINHO
            • guarda 200 no porquinho
            • quanto tenho guardado

            A categoria sai do que voce escreve: uber vira transporte, mercado
            vira alimentacao, farmacia vira saude.""";

    // O que o usuario mandou fazer por ultimo. Existe para frases de
    // continuacao funcionarem: depois de "guarde 500", um "mais 300" e um
    // comando completo na cabeca de quem escreve, mas sozinho nao diz o que
    // fazer com 300. Sem isto o assistente responde "nao entendi" a uma frase
    // que qualquer pessoa entenderia.
    private enum Acao { GUARDAR, RETIRAR, DESPESA, RECEITA }

    private record Ultima(Acao acao, String descricao, String categoria) {
    }

    // volatile porque a interface pode mandar comandos de threads diferentes.
    // Uma unica pessoa usa o aplicativo, entao guardar a ultima acao basta.
    private volatile Ultima ultima;

    private final FinancasTools financas;
    private final InvestimentoTools investimentos;
    private final AppTools app;
    private final FixosTools fixos;

    public InterpretadorDeComandos(FinancasTools financas, InvestimentoTools investimentos,
                                   AppTools app, FixosTools fixos) {
        this.financas = financas;
        this.investimentos = investimentos;
        this.app = app;
        this.fixos = fixos;
    }

    // Optional.empty() significa "nao entendi" — quem chama decide o que dizer.
    public Optional<String> interpretar(String comando) {
        if (comando == null || comando.isBlank()) {
            return Optional.empty();
        }
        String t = normalizar(comando);

        // Continuacao: "mais 300", "e mais 50", "outros 200", ou so um numero.
        // Repete a ultima acao com o valor novo.
        Optional<String> continuacao = tentarContinuacao(t);
        if (continuacao.isPresent()) {
            return continuacao;
        }

        // Sociais e ajuda vem primeiro: sao curtos e nao devem ser confundidos
        // com comando de dinheiro por conterem algum numero solto.
        if (contem(t, PEDIDOS_DE_AJUDA)) {
            return Optional.of(AJUDA);
        }
        if (contem(t, AGRADECIMENTOS)) {
            return Optional.of("De nada! Precisando, e so pedir.");
        }
        if (contem(t, DESPEDIDAS)) {
            return Optional.of("Ate mais! Seus lancamentos ficam salvos.");
        }
        if (t.length() <= 20 && contemPalavra(t, SAUDACOES)) {
            return Optional.of("Ola! Posso registrar gastos e receitas, consultar seu saldo e "
                    + "cuidar do porquinho. Diga algo como \"gastei 60 no uber\" ou pergunte "
                    + "\"qual e o meu saldo\".");
        }

        // Declarar que nao ha salario fixo. Precisa vir ANTES do bloco de itens
        // fixos: "nao tenho salario FIXO" contem a palavra fixo e seria lido
        // como cadastro de gasto fixo.
        if (contem(t, "salario", "renda") && contem(t, "nao tenho", "sem salario", "nao possuo",
                "nao recebo", "autonomo", "freelancer", "variavel", "desempregado")) {
            return Optional.of(financas.declararQueNaoTenhoSalario());
        }

        // --- ganhos e gastos fixos ---------------------------------------
        // Antes das regras genericas: "todo mes pago 120 de internet" tem valor
        // e verbo de gasto, e viraria um lancamento avulso sem esta checagem.
        boolean falaDeFixo = contem(t, "fixo", "todo mes", "todos os meses", "mensal", "recorrente",
                "toda semana", "conta de");

        // Fechar o mes e ver o que falta: antes da listagem, que e mais generica.
        if (contem(t, "fecha o mes", "fechar o mes", "fecha mes", "lanca tudo", "lancar tudo",
                "paga tudo", "pagar tudo")) {
            return Optional.of(fixos.fecharOMes());
        }
        if (contem(t, "falta", "faltam", "pendente", "vencendo", "a pagar", "devo pagar")
                || (contem(t, "como esta") && contem(t, "mes"))) {
            return Optional.of(fixos.contasDoMes());
        }
        if (contem(t, "lista", "listar", "quais", "meus", "ver") && falaDeFixo) {
            return Optional.of(fixos.listarFixos());
        }
        if (contem(t, "apag", "remov", "exclu") && falaDeFixo) {
            return idDe(t).map(fixos::apagarFixo);
        }
        if (contem(t, "paguei a", "paguei o", "veio", "chegou", "lanca", "lancar") && falaDeFixo) {
            Optional<Long> id = idDe(t);
            if (id.isPresent()) {
                return Optional.of(fixos.lancarFixo(id.get(), valorDepoisDe(t, "veio").orElse(null)));
            }
        }
        if (falaDeFixo && contem(t, "cadastr", "adicion", "registr", "criar", "tenho", "pago", "recebo", "sera")) {
            boolean ganho = contem(t, "recebo", "ganho", "entra", "recebimento", "aluguel que recebo");
            Optional<BigDecimal> valor = valorDe(t);
            Integer dia = diaDe(t);
            String descricao = descricaoDeItemFixo(t);
            String categoria = categoriaDe(t, ganho);
            if (categoria == null) {
                categoria = ganho ? "extra" : "outros";
            }
            return Optional.of(ganho
                    ? fixos.cadastrarGanhoFixo(descricao, categoria, valor.orElse(null), dia)
                    : fixos.cadastrarGastoFixo(descricao, categoria, valor.orElse(null), dia));
        }

        // --- tabela por modalidade ----------------------------------------
        if (contem(t, "resumo", "relatorio", "balanco", "tabela", "por categoria", "por modalidade",
                "onde estou gastando", "para onde", "no que gastei", "meus gastos totais")) {
            boolean tudo = contem(t, "tudo", "geral", "sempre", "total historico", "todos os meses");
            return Optional.of(fixos.resumoPorCategoria(!tudo));
        }


        // A ordem vai do mais especifico para o mais generico: "apagar movimento
        // do porquinho" precisa ser testado antes de "apagar transacao", e
        // ambos antes de qualquer coisa que so procure um valor.
        if (contem(t, "apag", "remov", "exclu", "delet") && contem(t, "movimento")) {
            return idDe(t).map(investimentos::apagarMovimentoDoPorquinho);
        }
        if (contem(t, "apag", "remov", "exclu", "delet") && contem(t, "transacao", "lancamento", "gasto", "despesa")) {
            return idDe(t).map(financas::apagarTransacao);
        }
        if (contem(t, "corrig", "atualiz", "muda", "alter", "troca") && contem(t, "transacao", "lancamento")) {
            Optional<Long> id = idDe(t);
            Optional<BigDecimal> valor = valorDepoisDe(t, "para");
            if (id.isPresent() && valor.isPresent()) {
                return Optional.of(financas.atualizarTransacao(id.get(), valor.get(), null, null, null));
            }
            return Optional.empty();
        }
        if (contem(t, "salario") && contem(t, "meu", "define", "definir", "e de", "ganho", "recebo")) {
            Optional<BigDecimal> valor = valorDe(t);
            if (valor.isPresent()) {
                return Optional.of(financas.definirSalario(valor.get(), diaDe(t)));
            }
        }
        if (contem(t, "salario")) {
            return Optional.of(financas.consultarSalario());
        }
        // O verbo sozinho basta: "guarde 500" e um comando completo, e exigir a
        // palavra "porquinho" fazia o assistente nao entender a forma mais
        // natural de pedir.
        if (contem(t, "porquinho", "guardad", "guarde", "guardar", "guardei", "guarda ",
                "poupanc", "poupar", "poupe", "reserva", "investiment", "investir", "invista",
                "tira ", "tirar", "tire ", "retira", "saca ", "sacar", "resgat")) {
            if (contem(t, "guard", "poupa", "investir", "separa", "reserva")) {
                Optional<BigDecimal> valor = valorDe(t);
                if (valor.isPresent()) {
                    String descricao = descricaoDoPorquinho(t, "Reserva");
                    lembrar(Acao.GUARDAR, descricao, null);
                    return Optional.of(investimentos.guardarNoPorquinho(descricao, valor.get()));
                }
            }
            if (contem(t, "tira", "retira", "saca", "resgat")) {
                Optional<BigDecimal> valor = valorDe(t);
                if (valor.isPresent()) {
                    String descricao = descricaoDoPorquinho(t, "Retirada");
                    lembrar(Acao.RETIRAR, descricao, null);
                    return Optional.of(investimentos.retirarDoPorquinho(descricao, valor.get()));
                }
            }
            if (contem(t, "movimento", "extrato", "lista")) {
                return Optional.of(investimentos.listarMovimentosDoPorquinho());
            }
            return Optional.of(investimentos.consultarPorquinho());
        }
        if (contem(t, "saldo", "no vermelho", "no azul", "como estou", "situacao", "resumo",
                "posso gastar", "quanto me resta", "estou bem")
                || (contem(t, "quanto") && contem(t, "tenho", "sobrou", "sobra", "resta"))) {
            return Optional.of(financas.consultarSaldo());
        }
        if ((contem(t, "quanto") && contem(t, "gast")) || contem(t, "gastos com", "gastos de",
                "total gasto", "quanto foi de")) {
            String categoria = categoriaDe(t, false);
            return Optional.of(financas.consultarGastoPorCategoria(categoria == null ? "alimentacao" : categoria));
        }
        if (contem(t, "lista", "listar", "extrato", "historico") || contem(t, "transacoes", "lancamentos")) {
            return Optional.of(financas.listarTransacoes());
        }
        if (contem(t, "quem fez", "quem criou", "autor", "desenvolvedor")) {
            return Optional.of(app.consultarAutor());
        }
        if (contem(t, "o que", "ajuda", "recursos", "funciona") && contem(t, "app", "aplicativo", "voce", "faz")) {
            return Optional.of(app.consultarRecursosDoApp());
        }

        // Lancamento: receita quando o verbo indica entrada, despesa no resto.
        Optional<BigDecimal> valor = valorDe(t);
        if (valor.isPresent()) {
            boolean receita = contem(t, "recebi", "ganhei", "entrou", "receita", "vendi", "caiu",
                    "faturei", "lucrei", "me pagaram", "pix de", "rendeu", "reembolso");
            if (receita || contem(t, "gastei", "paguei", "comprei", "gasto", "despesa", "torrei",
                    "saiu", "debitou", "custou", "transferi", "mandei", "doei", "assinei")) {
                String categoria = categoriaDe(t, receita);
                if (categoria == null) {
                    categoria = receita ? "extra" : "alimentacao";
                }
                String descricao = descricaoDe(t, null);
                lembrar(receita ? Acao.RECEITA : Acao.DESPESA, descricao, categoria);
                return Optional.of(financas.registrarTransacao(
                        descricao, valor.get(), categoria,
                        receita ? TipoTransacao.RECEITA : TipoTransacao.DESPESA));
            }
        }
        return Optional.empty();
    }

    // "mais 300" so faz sentido depois de outra coisa. So aceita frases curtas
    // e sem verbo proprio: "mais 300 no mercado" tem contexto suficiente para
    // seguir o caminho normal e virar um gasto novo.
    private Optional<String> tentarContinuacao(String t) {
        Ultima anterior = ultima;
        if (anterior == null) {
            return Optional.empty();
        }
        // Frase com verbo proprio ou categoria nao e continuacao: "gastei mais
        // 300 no mercado" diz o que e, e precisa virar um gasto de alimentacao
        // em vez de repetir o que veio antes.
        boolean temContextoProprio = contem(t, "gastei", "paguei", "comprei", "recebi", "ganhei",
                "guarda", "guarde", "guardar", "tira", "tirar", "retira", "salario", "porquinho",
                "saldo", "transacao", "fixo", "conta")
                || categoriaDe(t, false) != null
                || categoriaDe(t, true) != null;

        boolean pareceContinuacao = contem(t, "mais ", "e mais", "outros ", "outras ", "tambem ",
                "adiciona mais", "coloca mais", "poe mais")
                || t.trim().matches("^\\d+([.,]\\d{1,2})?$");

        if (!pareceContinuacao || temContextoProprio || t.length() > 40) {
            return Optional.empty();
        }
        Optional<BigDecimal> valor = valorDe(t);
        if (valor.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(repetir(anterior, valor.get()));
    }

    private String repetir(Ultima anterior, BigDecimal valor) {
        return switch (anterior.acao()) {
            case GUARDAR -> investimentos.guardarNoPorquinho(anterior.descricao(), valor);
            case RETIRAR -> investimentos.retirarDoPorquinho(anterior.descricao(), valor);
            case DESPESA -> financas.registrarTransacao(anterior.descricao(), valor,
                    anterior.categoria(), TipoTransacao.DESPESA);
            case RECEITA -> financas.registrarTransacao(anterior.descricao(), valor,
                    anterior.categoria(), TipoTransacao.RECEITA);
        };
    }

    private void lembrar(Acao acao, String descricao, String categoria) {
        ultima = new Ultima(acao, descricao, categoria);
    }

    // Minusculas e sem acento: "almoço" e "almoco" precisam casar igual.
    private static String normalizar(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return semAcento.toLowerCase(Locale.ROOT).trim();
    }

    private static boolean contem(String texto, String... termos) {
        for (String termo : termos) {
            if (texto.contains(termo)) {
                return true;
            }
        }
        return false;
    }

    private static Optional<BigDecimal> valorDe(String texto) {
        Matcher m = VALOR.matcher(texto);
        return m.find() ? Optional.of(paraNumero(m.group(1))) : Optional.empty();
    }

    // "corrige a transacao 3 para 60": o valor e o que vem DEPOIS do "para",
    // senao pegariamos o id como se fosse o valor.
    private static Optional<BigDecimal> valorDepoisDe(String texto, String marcador) {
        int i = texto.indexOf(marcador);
        return i < 0 ? Optional.empty() : valorDe(texto.substring(i + marcador.length()));
    }

    private static Optional<Long> idDe(String texto) {
        Matcher m = ID.matcher(texto);
        return m.find() ? Optional.of(Long.parseLong(m.group(1))) : Optional.empty();
    }

    private static Integer diaDe(String texto) {
        int i = texto.indexOf("dia ");
        if (i < 0) {
            return null;
        }
        Matcher m = ID.matcher(texto.substring(i));
        if (!m.find()) {
            return null;
        }
        int dia = Integer.parseInt(m.group(1));
        return dia >= 1 && dia <= 31 ? dia : null;
    }

    // Casa PALAVRA INTEIRA, nunca pedaco. Com busca por substring, "gas"
    // (moradia) casava dentro de "gastos" e "gastos com farmacia" virava
    // moradia. Palavras curtas colidem com palavras maiores o tempo todo:
    // "bar" em "barato", "moto" em "motorista", "luz" em "deslumbrante".
    private static String categoriaDe(String texto, boolean receita) {
        for (Map.Entry<Pattern, String> e : (receita ? PADROES_RECEITA : PADROES_DESPESA).entrySet()) {
            if (e.getKey().matcher(texto).find()) {
                return e.getValue();
            }
        }
        return null;
    }

    // Pre-compilado uma vez: montar dezenas de padroes a cada comando seria
    // desperdicio, e a ordem de insercao precisa ser preservada.
    private static Map<Pattern, String> compilar(Map<String, String> palavras) {
        Map<Pattern, String> padroes = new LinkedHashMap<>();
        palavras.forEach((palavra, categoria) ->
                padroes.put(Pattern.compile("\\b" + Pattern.quote(palavra.trim()) + "\\b"), categoria));
        return padroes;
    }

    private static boolean contemPalavra(String texto, String... termos) {
        for (String termo : termos) {
            if (Pattern.compile("\\b" + Pattern.quote(termo.trim()) + "\\b").matcher(texto).find()) {
                return true;
            }
        }
        return false;
    }

    // A descricao e o texto util depois da preposicao: em "gastei 60 no uber",
    // e "uber". Sem isso todo lancamento se chamaria "gastei 60 no uber".
    private static String descricaoDe(String texto, String remover) {
        String limpo = texto.replaceAll(VALOR.pattern(), " ")
                .replaceAll("\\b(reais?|r\\$|conto|pila)\\b", " ")
                .replaceAll("\\b(gastei|paguei|comprei|recebi|ganhei|entrou|vendi|caiu|torrei|guarda|guardar|guardei|poupar|investir|separa|tira|tirar|retira|retirar|saca|sacar|resgatar|no|na|em|de|do|da|com|para|pra|o|a|um|uma|meu|minha)\\b", " ");
        if (remover != null) {
            limpo = limpo.replace(remover, " ");
        }
        String descricao = limpo.replaceAll("\\s+", " ").trim();
        if (descricao.isEmpty()) {
            return "Lancamento";
        }
        return Character.toUpperCase(descricao.charAt(0)) + descricao.substring(1);
    }

    // "todo mes pago 89 de internet" precisa virar "Internet", nao "Todo mes
    // pago internet": a descricao aparece em toda listagem e em todo lancamento
    // gerado por este item.
    private static String descricaoDeItemFixo(String texto) {
        String limpo = texto.replaceAll(
                "\\b(todo|todos|os|mes|meses|mensal|mensalmente|recorrente|fixo|fixa|conta|de|do|da|"
                + "pago|pagar|recebo|receber|tenho|cadastra|cadastrar|adiciona|adicionar|registra|"
                + "registrar|criar|sera|toda|semana)\\b", " ");
        return descricaoDe(limpo, null);
    }

    // O motivo de guardar, quando o usuario diz um ("para a viagem"). Sem
    // motivo, um rotulo neutro: "guarde 500" virava a descricao "Guarde", que
    // aparecia feio em toda a listagem de movimentos.
    private static String descricaoDoPorquinho(String texto, String padrao) {
        String limpo = texto.replaceAll(
                "\\b(guarda|guarde|guardar|guardei|guardado|poupar|poupe|poupanca|investir|"
                + "invista|investimento|separa|separar|tira|tire|tirar|retira|retirar|saca|sacar|"
                + "resgata|resgatar|porquinho|quero|preciso|no|na|do|da|de|em|para|pra|o|a|um|uma)\\b",
                " ");
        String descricao = descricaoDe(limpo, null);
        return descricao.isBlank() || descricao.equals("Lancamento") ? padrao : descricao;
    }

    private static BigDecimal paraNumero(String bruto) {
        String limpo = bruto.contains(",")
                ? bruto.replace(".", "").replace(",", ".")
                : bruto;
        return new BigDecimal(limpo);
    }
}
