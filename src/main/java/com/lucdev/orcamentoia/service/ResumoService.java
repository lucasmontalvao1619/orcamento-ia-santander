package com.lucdev.orcamentoia.service;

import com.lucdev.orcamentoia.model.TipoTransacao;
import com.lucdev.orcamentoia.model.Transacao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Monta a tabela de gastos por modalidade.
//
// Nada e guardado: o resumo e calculado no momento em que o usuario pede. Uma
// tabela materializada precisaria ser atualizada a cada lancamento, correcao e
// exclusao — e ficaria errada no dia em que alguem esquecesse de atualizar.
@Service
public class ResumoService {

    private final TransacaoService transacaoService;

    public ResumoService(TransacaoService transacaoService) {
        this.transacaoService = transacaoService;
    }

    public record LinhaDoResumo(
            String categoria,
            BigDecimal total,
            long lancamentos,
            // Quanto esta categoria representa do total gasto. E o que responde
            // "para onde meu dinheiro esta indo".
            BigDecimal percentual
    ) {
    }

    public record Resumo(
            String periodo,
            BigDecimal totalDespesas,
            BigDecimal totalReceitas,
            BigDecimal saldo,
            List<LinhaDoResumo> despesasPorCategoria,
            List<LinhaDoResumo> receitasPorCategoria
    ) {
    }

    // mesAtual=true limita ao mes corrente, que e como as pessoas pensam o
    // orcamento. Sem o filtro, o resumo mistura meses e nao serve para decidir
    // nada sobre o mes que esta correndo.
    public Resumo montar(boolean apenasMesAtual) {
        List<Transacao> transacoes = transacaoService.listarTodas();
        if (apenasMesAtual) {
            LocalDate hoje = LocalDate.now();
            transacoes = transacoes.stream()
                    .filter(t -> t.getDataHora() != null
                            && t.getDataHora().getYear() == hoje.getYear()
                            && t.getDataHora().getMonthValue() == hoje.getMonthValue())
                    .toList();
        }

        BigDecimal totalDespesas = somar(transacoes, TipoTransacao.DESPESA);
        BigDecimal totalReceitas = somar(transacoes, TipoTransacao.RECEITA);

        return new Resumo(
                apenasMesAtual ? "mes atual" : "tudo",
                totalDespesas,
                totalReceitas,
                totalReceitas.subtract(totalDespesas),
                agrupar(transacoes, TipoTransacao.DESPESA, totalDespesas),
                agrupar(transacoes, TipoTransacao.RECEITA, totalReceitas));
    }

    private BigDecimal somar(List<Transacao> transacoes, TipoTransacao tipo) {
        return transacoes.stream()
                .filter(t -> t.getTipo() == tipo)
                .map(Transacao::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<LinhaDoResumo> agrupar(List<Transacao> transacoes, TipoTransacao tipo, BigDecimal total) {
        Map<String, List<Transacao>> porCategoria = new LinkedHashMap<>();
        for (Transacao t : transacoes) {
            if (t.getTipo() == tipo) {
                porCategoria.computeIfAbsent(
                        t.getCategoria() == null ? "outros" : t.getCategoria(),
                        c -> new ArrayList<>()).add(t);
            }
        }

        List<LinhaDoResumo> linhas = new ArrayList<>();
        porCategoria.forEach((categoria, itens) -> {
            BigDecimal soma = itens.stream().map(Transacao::getValor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal percentual = total.signum() == 0
                    ? BigDecimal.ZERO
                    : soma.multiply(BigDecimal.valueOf(100))
                            .divide(total, 1, RoundingMode.HALF_EVEN);
            linhas.add(new LinhaDoResumo(categoria, soma, itens.size(), percentual));
        });

        // Maior gasto primeiro: e a informacao que a pessoa procura ao abrir a
        // tabela, e nao a ordem alfabetica das categorias.
        linhas.sort(Comparator.comparing(LinhaDoResumo::total).reversed());
        return linhas;
    }
}
