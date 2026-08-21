package com.lucdev.orcamentoia.tool;

import com.lucdev.orcamentoia.exception.RecursoNaoEncontradoException;
import com.lucdev.orcamentoia.model.Investimento;
import com.lucdev.orcamentoia.model.TipoInvestimento;
import com.lucdev.orcamentoia.service.InvestimentoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

// O InvestimentoServiceTest cobre a matematica do rendimento. Aqui o alvo e
// outro: o texto que volta para o usuario, que e a resposta que ele le quando
// fala com o assistente.
@ExtendWith(MockitoExtension.class)
class InvestimentoToolsTest {

    @Mock
    private InvestimentoService investimentoService;

    @InjectMocks
    private InvestimentoTools investimentoTools;

    @Test
    void guardarConfirmaOValorEOTotalAtualizado() {
        when(investimentoService.registrar(eq("Viagem"), eq(new BigDecimal("800.00")), eq(TipoInvestimento.APORTE)))
                .thenReturn(movimento("Viagem", "800.00", TipoInvestimento.APORTE));
        when(investimentoService.calcularSaldoComRendimento()).thenReturn(new BigDecimal("1234.5678"));

        String resposta = investimentoTools.guardarNoPorquinho("Viagem", new BigDecimal("800.00"));

        assertThat(resposta).contains("800,00", "Viagem", "1234,57");
    }

    @Test
    void retirarConfirmaOValorEOTotalAtualizado() {
        when(investimentoService.registrar(eq("Conserto"), eq(new BigDecimal("200.00")), eq(TipoInvestimento.RETIRADA)))
                .thenReturn(movimento("Conserto", "200.00", TipoInvestimento.RETIRADA));
        when(investimentoService.calcularSaldoComRendimento()).thenReturn(new BigDecimal("600.00"));

        String resposta = investimentoTools.retirarDoPorquinho("Conserto", new BigDecimal("200.00"));

        assertThat(resposta).contains("200,00", "Conserto", "600,00");
    }

    @Test
    void guardarPropagaAValidacaoDeValor() {
        when(investimentoService.registrar(eq("Invalido"), eq(new BigDecimal("-1.00")), eq(TipoInvestimento.APORTE)))
                .thenThrow(new IllegalArgumentException("O valor do investimento deve ser positivo."));

        assertThatThrownBy(() -> investimentoTools.guardarNoPorquinho("Invalido", new BigDecimal("-1.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listarMovimentosAvisaQuandoOPorquinhoNuncaFoiUsado() {
        when(investimentoService.listarTodos()).thenReturn(List.of());

        assertThat(investimentoTools.listarMovimentosDoPorquinho()).contains("Nenhum movimento");
    }

    // O id entre colchetes e o que o modelo le para chamar apagarMovimento
    // depois: se sumir da listagem, apagar por voz para de funcionar.
    @Test
    void listarMovimentosMostraOIdDeCadaUm() {
        when(investimentoService.listarTodos()).thenReturn(List.of(
                comId(1L, movimento("Reserva", "500.00", TipoInvestimento.APORTE)),
                comId(2L, movimento("Conserto", "120.00", TipoInvestimento.RETIRADA))));

        String resposta = investimentoTools.listarMovimentosDoPorquinho();

        assertThat(resposta.lines()).hasSize(3);
        assertThat(resposta).contains("[1]", "[2]", "APORTE", "RETIRADA", "500,00", "120,00");
    }

    @Test
    void apagarMovimentoConfirmaOQueSaiuEOTotalRestante() {
        when(investimentoService.apagar(2L))
                .thenReturn(comId(2L, movimento("Engano", "120.00", TipoInvestimento.RETIRADA)));
        when(investimentoService.calcularSaldoComRendimento()).thenReturn(new BigDecimal("500.00"));

        String resposta = investimentoTools.apagarMovimentoDoPorquinho(2L);

        assertThat(resposta).contains("RETIRADA", "120,00", "500,00");
    }

    @Test
    void apagarMovimentoPropagaIdInexistente() {
        when(investimentoService.apagar(99L))
                .thenThrow(new RecursoNaoEncontradoException("Nao existe movimento com o id 99."));

        assertThatThrownBy(() -> investimentoTools.apagarMovimentoDoPorquinho(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void consultarPorquinhoAvisaQuandoEstaVazio() {
        when(investimentoService.calcularTotal()).thenReturn(BigDecimal.ZERO);
        when(investimentoService.calcularRendimento()).thenReturn(BigDecimal.ZERO);
        when(investimentoService.calcularSaldoComRendimento()).thenReturn(BigDecimal.ZERO);
        when(investimentoService.getCdiAnual()).thenReturn(new BigDecimal("0.1065"));

        assertThat(investimentoTools.consultarPorquinho()).contains("vazio");
    }

    // Depositado e rendimento aparecem separados de proposito: o usuario precisa
    // enxergar quanto do total ele colocou e quanto o dinheiro rendeu sozinho.
    @Test
    void consultarPorquinhoSeparaODepositadoDoRendimento() {
        when(investimentoService.calcularTotal()).thenReturn(new BigDecimal("1000.00"));
        when(investimentoService.calcularRendimento()).thenReturn(new BigDecimal("34.5678"));
        when(investimentoService.calcularSaldoComRendimento()).thenReturn(new BigDecimal("1034.5678"));
        when(investimentoService.getCdiAnual()).thenReturn(new BigDecimal("0.1065"));

        String resposta = investimentoTools.consultarPorquinho();

        assertThat(resposta).contains("1034,57", "1000,00", "34,57", "10,65");
    }

    private Investimento movimento(String descricao, String valor, TipoInvestimento tipo) {
        return new Investimento(descricao, new BigDecimal(valor), tipo);
    }

    private Investimento comId(Long id, Investimento investimento) {
        investimento.setId(id);
        return investimento;
    }
}
