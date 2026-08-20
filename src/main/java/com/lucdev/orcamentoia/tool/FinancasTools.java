package com.lucdev.orcamentoia.tool;

import com.lucdev.orcamentoia.model.TipoTransacao;
import com.lucdev.orcamentoia.model.Transacao;
import com.lucdev.orcamentoia.service.TransacaoService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Component
public class FinancasTools {

    // O texto destas ferramentas volta para o usuario final, entao o locale e
    // fixo: sem isso o valor sairia formatado conforme a maquina que roda a app.
    private static final Locale BR = Locale.forLanguageTag("pt-BR");

    private final TransacaoService transacaoService;

    public FinancasTools(TransacaoService transacaoService) {
        this.transacaoService = transacaoService;
    }

    @Tool(description = "Registra uma nova transacao financeira (receita ou despesa) no orcamento do usuario.")
    public String registrarTransacao(
            @ToolParam(description = "Descricao curta da transacao, ex: 'almoco no restaurante'") String descricao,
            @ToolParam(description = "Valor da transacao em reais, sempre positivo") BigDecimal valor,
            @ToolParam(description = "Categoria da transacao, ex: 'alimentacao', 'transporte', 'salario'") String categoria,
            @ToolParam(description = "Tipo da transacao: RECEITA para entradas de dinheiro, DESPESA para saidas") TipoTransacao tipo) {

        Transacao transacao = transacaoService.registrar(descricao, valor, categoria, tipo);
        return String.format(BR, "Transacao registrada com sucesso. ID %d, %s de R$ %.2f na categoria %s.",
                transacao.getId(), transacao.getTipo(), transacao.getValor(), transacao.getCategoria());
    }

    @Tool(description = "Consulta o saldo atual do orcamento, calculado como a soma das receitas menos a soma das despesas.")
    public String consultarSaldo() {
        BigDecimal saldo = transacaoService.calcularSaldo();
        return String.format(BR, "O saldo atual do orcamento e de R$ %.2f.", saldo);
    }

    @Tool(description = "Consulta o total gasto em uma categoria especifica de despesas.")
    public String consultarGastoPorCategoria(
            @ToolParam(description = "Nome da categoria a consultar, ex: 'alimentacao'") String categoria) {

        BigDecimal total = transacaoService.listarPorCategoria(categoria).stream()
                .filter(t -> t.getTipo() == TipoTransacao.DESPESA)
                .map(Transacao::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return String.format(BR, "O total de despesas na categoria %s e de R$ %.2f.", categoria, total);
    }

    @Tool(description = "Lista as ultimas transacoes registradas no orcamento.")
    public String listarTransacoes() {
        List<Transacao> transacoes = transacaoService.listarTodas();
        if (transacoes.isEmpty()) {
            return "Nenhuma transacao registrada ate o momento.";
        }
        StringBuilder resultado = new StringBuilder("Transacoes registradas:\n");
        for (Transacao t : transacoes) {
            resultado.append(String.format(BR, "- %s | %s | R$ %.2f | %s%n",
                    t.getTipo(), t.getDescricao(), t.getValor(), t.getCategoria()));
        }
        return resultado.toString();
    }
}
