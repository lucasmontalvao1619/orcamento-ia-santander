package com.lucdev.orcamentoia.tool;

import com.lucdev.orcamentoia.model.Investimento;
import com.lucdev.orcamentoia.model.TipoInvestimento;
import com.lucdev.orcamentoia.service.InvestimentoService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

// O porquinho tambem precisa estar ao alcance do assistente: sem estas
// ferramentas, "guarda 200 no porquinho" caia em registrarTransacao e viraria
// uma despesa do orcamento, que e outra coisa.
@Component
public class InvestimentoTools {

    private static final Locale BR = Locale.forLanguageTag("pt-BR");

    private final InvestimentoService investimentoService;

    public InvestimentoTools(InvestimentoService investimentoService) {
        this.investimentoService = investimentoService;
    }

    @Tool(description = "Guarda dinheiro no porquinho de investimento. Use quando o usuario disser "
            + "que quer guardar, poupar, investir ou reservar um valor. O porquinho e separado do "
            + "saldo do orcamento e rende 100% do CDI.")
    public String guardarNoPorquinho(
            @ToolParam(description = "Motivo de estar guardando, ex: 'reserva de emergencia'") String descricao,
            @ToolParam(description = "Valor a guardar em reais, sempre positivo") BigDecimal valor) {

        Investimento investimento = investimentoService.registrar(descricao, valor, TipoInvestimento.APORTE);
        return String.format(BR, "Guardei R$ %.2f no porquinho (%s). Total agora: R$ %.2f.",
                investimento.getValor(), investimento.getDescricao(), arredondar(saldo()));
    }

    @Tool(description = "Retira dinheiro do porquinho de investimento. Use quando o usuario disser "
            + "que quer tirar, sacar ou resgatar um valor guardado.")
    public String retirarDoPorquinho(
            @ToolParam(description = "Motivo da retirada, ex: 'conserto do carro'") String descricao,
            @ToolParam(description = "Valor a retirar em reais, sempre positivo") BigDecimal valor) {

        Investimento investimento = investimentoService.registrar(descricao, valor, TipoInvestimento.RETIRADA);
        return String.format(BR, "Retirei R$ %.2f do porquinho (%s). Total agora: R$ %.2f.",
                investimento.getValor(), investimento.getDescricao(), arredondar(saldo()));
    }

    @Tool(description = "Lista os movimentos do porquinho com o id de cada um.")
    public String listarMovimentosDoPorquinho() {
        var movimentos = investimentoService.listarTodos();
        if (movimentos.isEmpty()) {
            return "Nenhum movimento registrado no porquinho.";
        }
        StringBuilder resultado = new StringBuilder("Movimentos do porquinho:\n");
        for (Investimento i : movimentos) {
            resultado.append(String.format(BR, "- [%d] %s | %s | R$ %.2f%n",
                    i.getId(), i.getTipo(), i.getDescricao(), i.getValor()));
        }
        return resultado.toString();
    }

    @Tool(description = "Apaga um movimento do porquinho pelo id. Use quando o usuario pedir para "
            + "desfazer, apagar ou corrigir um deposito ou retirada. Chame listarMovimentosDoPorquinho "
            + "antes se nao souber o id.")
    public String apagarMovimentoDoPorquinho(
            @ToolParam(description = "Id do movimento a apagar") Long id) {

        Investimento investimento = investimentoService.apagar(id);
        return String.format(BR, "Movimento apagado: %s de R$ %.2f. Total agora: R$ %.2f.",
                investimento.getTipo(), investimento.getValor(), arredondar(saldo()));
    }

    @Tool(description = "Consulta quanto existe guardado no porquinho de investimento, incluindo "
            + "quanto ja rendeu de juros a 100% do CDI.")
    public String consultarPorquinho() {
        BigDecimal depositado = investimentoService.calcularTotal();
        BigDecimal rendimento = investimentoService.calcularRendimento();
        BigDecimal total = investimentoService.calcularSaldoComRendimento();
        BigDecimal taxa = investimentoService.getCdiAnual().multiply(BigDecimal.valueOf(100));

        if (depositado.signum() == 0 && total.signum() == 0) {
            return "O porquinho esta vazio. Nada foi guardado ainda.";
        }
        return String.format(BR,
                "No porquinho existem R$ %.2f: R$ %.2f depositados mais R$ %.2f de rendimento, "
                        + "a 100%% do CDI (%.2f%% ao ano).",
                arredondar(total), arredondar(depositado), arredondar(rendimento), taxa);
    }

    private BigDecimal saldo() {
        return investimentoService.calcularSaldoComRendimento();
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
