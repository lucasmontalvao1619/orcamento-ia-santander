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
    private static final Map<String, String> CATEGORIAS = new LinkedHashMap<>();

    static {
        for (String p : new String[]{"uber", "taxi", "onibus", "metro", "gasolina", "combustivel",
                "passagem", "estacionamento", "corrida", "transporte"}) CATEGORIAS.put(p, "transporte");
        for (String p : new String[]{"almoco", "jantar", "lanche", "cafe", "mercado", "supermercado",
                "padaria", "restaurante", "ifood", "pizza", "comida", "feira", "alimentacao"}) CATEGORIAS.put(p, "alimentacao");
        for (String p : new String[]{"aluguel", "condominio", "luz", "agua", "internet", "gas",
                "faxina", "casa", "moradia"}) CATEGORIAS.put(p, "moradia");
        for (String p : new String[]{"cinema", "bar", "show", "viagem", "jogo", "netflix", "spotify",
                "festa", "lazer"}) CATEGORIAS.put(p, "lazer");
        for (String p : new String[]{"farmacia", "remedio", "medico", "dentista", "exame", "consulta",
                "academia", "saude"}) CATEGORIAS.put(p, "saude");
    }

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
        if (contem(t, "saldo") || (contem(t, "quanto") && contem(t, "tenho", "sobrou", "sobra"))) {
            return Optional.of(financas.consultarSaldo());
        }
        if (contem(t, "quanto") && contem(t, "gast")) {
            String categoria = categoriaDe(t);
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
            boolean receita = contem(t, "recebi", "ganhei", "entrou", "receita", "vendi", "caiu");
            if (receita || contem(t, "gastei", "paguei", "comprei", "gasto", "despesa", "torrei")) {
                String categoria = categoriaDe(t);
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

    private static String categoriaDe(String texto) {
        for (Map.Entry<String, String> e : CATEGORIAS.entrySet()) {
            if (texto.contains(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
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
