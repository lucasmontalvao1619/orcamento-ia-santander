package com.lucdev.orcamentoia.tool;

import com.lucdev.orcamentoia.model.Recorrente;
import com.lucdev.orcamentoia.model.TipoTransacao;
import com.lucdev.orcamentoia.model.Transacao;
import com.lucdev.orcamentoia.service.RecorrenteService;
import com.lucdev.orcamentoia.service.ResumoService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

// Ganhos e gastos fixos, e a tabela por modalidade.
//
// Existe pela Regra 1 do projeto: recurso que a interface tem e o assistente
// nao alcanca esta pela metade.
@Component
public class FixosTools {

    private static final Locale BR = Locale.forLanguageTag("pt-BR");

    private final RecorrenteService recorrentes;
    private final ResumoService resumos;

    public FixosTools(RecorrenteService recorrentes, ResumoService resumos) {
        this.recorrentes = recorrentes;
        this.resumos = resumos;
    }

    @Tool(description = "Cadastra um gasto fixo mensal: aluguel, luz, agua, internet, cartao de "
            + "credito, mensalidade. Use quando o usuario disser que paga algo todo mes. O valor e "
            + "uma PREVISAO e pode ficar vazio para contas que variam, como cartao e luz.")
    public String cadastrarGastoFixo(
            @ToolParam(description = "Nome da conta, ex: 'conta de luz'") String descricao,
            @ToolParam(required = false, description = "Categoria: alimentacao, transporte, moradia, lazer ou saude") String categoria,
            @ToolParam(required = false, description = "Valor previsto por mes, se o usuario souber") BigDecimal valorPrevisto,
            @ToolParam(required = false, description = "Dia do vencimento, de 1 a 31") Integer diaVencimento) {

        Recorrente r = recorrentes.criar(descricao, categoria, TipoTransacao.DESPESA, valorPrevisto, diaVencimento);
        return descrever("Gasto fixo cadastrado", r);
    }

    @Tool(description = "Cadastra um ganho fixo mensal alem do salario: aluguel que o usuario "
            + "recebe, pensao, aposentadoria, renda de aplicacao, mesada.")
    public String cadastrarGanhoFixo(
            @ToolParam(description = "Nome do ganho, ex: 'aluguel do apartamento'") String descricao,
            @ToolParam(required = false, description = "Categoria: salario, presente ou extra") String categoria,
            @ToolParam(required = false, description = "Valor previsto por mes") BigDecimal valorPrevisto,
            @ToolParam(required = false, description = "Dia em que cai, de 1 a 31") Integer diaVencimento) {

        Recorrente r = recorrentes.criar(descricao, categoria, TipoTransacao.RECEITA, valorPrevisto, diaVencimento);
        return descrever("Ganho fixo cadastrado", r);
    }

    @Tool(description = "Lista os ganhos e gastos fixos cadastrados, com o id de cada um e quanto "
            + "se espera pagar e receber por mes.")
    public String listarFixos() {
        List<Recorrente> itens = recorrentes.listarTodos();
        if (itens.isEmpty()) {
            return "Nenhum ganho ou gasto fixo cadastrado ainda.";
        }
        StringBuilder r = new StringBuilder("Ganhos e gastos fixos:\n");
        for (Recorrente i : itens) {
            r.append(String.format(BR, "- [%d] %s | %s | %s | %s%n",
                    i.getId(), i.getTipo(), i.getDescricao(),
                    i.getValorPrevisto() == null
                            ? "valor variavel"
                            : String.format(BR, "previsto R$ %.2f", i.getValorPrevisto()),
                    i.getDiaVencimento() == null ? "sem dia" : "dia " + i.getDiaVencimento()));
        }
        r.append(String.format(BR, "Previsto por mes: R$ %.2f de gastos e R$ %.2f de ganhos.",
                recorrentes.totalPrevisto(TipoTransacao.DESPESA),
                recorrentes.totalPrevisto(TipoTransacao.RECEITA)));
        long variaveis = recorrentes.quantidadeSemPrevisao(TipoTransacao.DESPESA);
        if (variaveis > 0) {
            r.append(String.format(BR, " Fora %d conta(s) de valor variavel.", variaveis));
        }
        return r.toString();
    }

    @Tool(description = "Lanca um ganho ou gasto fixo no orcamento, com o valor que realmente veio "
            + "neste mes. Use quando o usuario disser que pagou ou recebeu um item fixo, ex: 'paguei "
            + "a luz, veio 143'. Sem valor informado, usa a previsao cadastrada. Se nao souber o id, "
            + "chame listarFixos antes.")
    public String lancarFixo(
            @ToolParam(description = "Id do item fixo") Long id,
            @ToolParam(required = false, description = "Valor real deste mes; vazio usa a previsao") BigDecimal valor) {

        Transacao t = recorrentes.lancar(id, valor);
        return String.format(BR, "Lancado: %s de R$ %.2f na categoria %s (%s).",
                t.getTipo(), t.getValor(), t.getCategoria(), t.getDescricao());
    }

    @Tool(description = "Apaga um ganho ou gasto fixo pelo id. Nao apaga os lancamentos ja feitos.")
    public String apagarFixo(@ToolParam(description = "Id do item fixo") Long id) {
        Recorrente r = recorrentes.apagar(id);
        return "Item fixo apagado: " + r.getDescricao() + ". Os lancamentos ja feitos continuam no orcamento.";
    }

    @Tool(description = "Mostra a tabela de gastos por modalidade: quanto foi gasto em cada "
            + "categoria, quantos lancamentos e quanto isso representa do total. Use quando "
            + "perguntarem para onde o dinheiro esta indo, um resumo, um balanco ou um relatorio.")
    public String resumoPorCategoria(
            @ToolParam(required = false, description = "true para so o mes atual, false para tudo") Boolean apenasMesAtual) {

        ResumoService.Resumo resumo = resumos.montar(apenasMesAtual == null || apenasMesAtual);
        if (resumo.despesasPorCategoria().isEmpty() && resumo.receitasPorCategoria().isEmpty()) {
            return "Nenhum lancamento no periodo (" + resumo.periodo() + ").";
        }
        StringBuilder r = new StringBuilder("Resumo (" + resumo.periodo() + "):\n");
        if (!resumo.despesasPorCategoria().isEmpty()) {
            r.append("Gastos por modalidade:\n");
            for (ResumoService.LinhaDoResumo l : resumo.despesasPorCategoria()) {
                r.append(String.format(BR, "- %s: R$ %.2f (%.1f%%, %d lancamento(s))%n",
                        l.categoria(), l.total(), l.percentual(), l.lancamentos()));
            }
        }
        r.append(String.format(BR, "Total de gastos: R$ %.2f. Total de receitas: R$ %.2f. Saldo: R$ %.2f.",
                resumo.totalDespesas(), resumo.totalReceitas(), resumo.saldo()));
        return r.toString();
    }

    private String descrever(String prefixo, Recorrente r) {
        return String.format(BR, "%s: %s, categoria %s, %s, %s.",
                prefixo, r.getDescricao(), r.getCategoria(),
                r.getValorPrevisto() == null
                        ? "valor variavel (voce informa ao lancar)"
                        : String.format(BR, "previsto R$ %.2f", r.getValorPrevisto()),
                r.getDiaVencimento() == null ? "sem dia definido" : "todo dia " + r.getDiaVencimento());
    }
}
