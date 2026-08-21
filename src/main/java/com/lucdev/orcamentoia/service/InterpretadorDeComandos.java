package com.lucdev.orcamentoia.service;

import com.lucdev.orcamentoia.model.TipoTransacao;
import com.lucdev.orcamentoia.tool.AppTools;
import com.lucdev.orcamentoia.tool.FinancasTools;
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
// Um recurso que so a IA alcanca fica invisivel para quem nao tem chave — veja
// a regra registrada no CLAUDE.md do projeto.
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

    static final String AJUDA = """
            Posso cuidar do seu orcamento por comandos escritos. Exemplos:

            Lancamentos
              gastei 60 no uber          recebi 500 de freela
              paguei 1.250,90 de aluguel   ganhei 200 de presente
              corrige a transacao 3 para 45
              apaga a transacao 3

            Consultas
              qual e o meu saldo         quanto gastei com alimentacao
              listar transacoes          como estou

            Salario
              meu salario e 3000, dia 15   qual e o meu salario
              nao tenho salario fixo

            Porquinho
              guarda 200 no porquinho    tira 100 do porquinho
              quanto tenho guardado      movimentos do porquinho
              apaga o movimento 2

            As categorias sao inferidas pelo que voce escreve: uber vira
            transporte, mercado vira alimentacao, farmacia vira saude.""";

    private final FinancasTools financas;
    private final InvestimentoTools investimentos;
    private final AppTools app;

    public InterpretadorDeComandos(FinancasTools financas, InvestimentoTools investimentos, AppTools app) {
        this.financas = financas;
        this.investimentos = investimentos;
        this.app = app;
    }

    // Optional.empty() significa "nao entendi" — quem chama decide o que dizer.
    public Optional<String> interpretar(String comando) {
        if (comando == null || comando.isBlank()) {
            return Optional.empty();
        }
        String t = normalizar(comando);

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

        // Declarar que nao ha salario fixo: antes das regras de salario, senao
        // "nao tenho salario" cairia na consulta de salario.
        if (contem(t, "salario", "renda") && contem(t, "nao tenho", "sem salario", "nao possuo",
                "nao recebo", "autonomo", "freelancer", "variavel", "desempregado")) {
            return Optional.of(financas.declararQueNaoTenhoSalario());
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
        if (contem(t, "porquinho", "guardad", "poupanc", "reserva", "investiment")) {
            if (contem(t, "guard", "poupa", "investir", "separa", "reserva")) {
                Optional<BigDecimal> valor = valorDe(t);
                if (valor.isPresent()) {
                    return Optional.of(investimentos.guardarNoPorquinho(descricaoDe(t, "porquinho"), valor.get()));
                }
            }
            if (contem(t, "tira", "retira", "saca", "resgat")) {
                Optional<BigDecimal> valor = valorDe(t);
                if (valor.isPresent()) {
                    return Optional.of(investimentos.retirarDoPorquinho(descricaoDe(t, "porquinho"), valor.get()));
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
                return Optional.of(financas.registrarTransacao(
                        descricaoDe(t, null), valor.get(), categoria,
                        receita ? TipoTransacao.RECEITA : TipoTransacao.DESPESA));
            }
        }
        return Optional.empty();
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

    private static BigDecimal paraNumero(String bruto) {
        String limpo = bruto.contains(",")
                ? bruto.replace(".", "").replace(",", ".")
                : bruto;
        return new BigDecimal(limpo);
    }
}
