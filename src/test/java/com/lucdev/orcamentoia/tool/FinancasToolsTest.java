package com.lucdev.orcamentoia.tool;

import com.lucdev.orcamentoia.model.TipoTransacao;
import com.lucdev.orcamentoia.model.Transacao;
import com.lucdev.orcamentoia.service.TransacaoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

// As tools sao o que o modelo de linguagem executa de verdade: o texto que elas
// devolvem vira a resposta ao usuario, entao vale testar o conteudo.
@ExtendWith(MockitoExtension.class)
class FinancasToolsTest {

    @Mock
    private TransacaoService transacaoService;

    @InjectMocks
    private FinancasTools financasTools;

    @Test
    void registrarTransacaoConfirmaOQueFoiGravado() {
        Transacao gravada = comId(7L, transacao("Almoco", "50.00", "alimentacao", TipoTransacao.DESPESA));
        when(transacaoService.registrar(eq("Almoco"), any(), eq("alimentacao"), eq(TipoTransacao.DESPESA)))
                .thenReturn(gravada);

        String resposta = financasTools.registrarTransacao(
                "Almoco", new BigDecimal("50.00"), "alimentacao", TipoTransacao.DESPESA);

        assertThat(resposta).contains("7", "DESPESA", "50,00", "alimentacao");
    }

    @Test
    void registrarTransacaoPropagaAValidacaoDeValor() {
        when(transacaoService.registrar(any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("O valor da transacao deve ser positivo."));

        assertThatThrownBy(() -> financasTools.registrarTransacao(
                "Invalida", new BigDecimal("-1.00"), "erro", TipoTransacao.DESPESA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void consultarSaldoFormataOValorEmReais() {
        when(transacaoService.calcularSaldo()).thenReturn(new BigDecimal("2490.50"));

        assertThat(financasTools.consultarSaldo()).contains("2490,50");
    }

    // Receitas na mesma categoria nao podem entrar na conta de gastos.
    @Test
    void consultarGastoPorCategoriaSomaApenasDespesas() {
        when(transacaoService.listarPorCategoria("alimentacao")).thenReturn(List.of(
                transacao("Almoco", "50.00", "alimentacao", TipoTransacao.DESPESA),
                transacao("Jantar", "30.00", "alimentacao", TipoTransacao.DESPESA),
                transacao("Estorno", "20.00", "alimentacao", TipoTransacao.RECEITA)));

        assertThat(financasTools.consultarGastoPorCategoria("alimentacao")).contains("80,00");
    }

    @Test
    void listarTransacoesAvisaQuandoNaoHaNada() {
        when(transacaoService.listarTodas()).thenReturn(List.of());

        assertThat(financasTools.listarTransacoes()).contains("Nenhuma transacao");
    }

    @Test
    void listarTransacoesTrazUmaLinhaPorTransacao() {
        when(transacaoService.listarTodas()).thenReturn(List.of(
                transacao("Salario", "3000.00", "salario", TipoTransacao.RECEITA),
                transacao("Mercado", "500.00", "alimentacao", TipoTransacao.DESPESA)));

        String resposta = financasTools.listarTransacoes();

        assertThat(resposta.lines()).hasSize(3);
        assertThat(resposta).contains("Salario", "3000,00", "Mercado", "500,00");
    }

    private Transacao transacao(String descricao, String valor, String categoria, TipoTransacao tipo) {
        return new Transacao(descricao, new BigDecimal(valor), categoria, tipo);
    }

    private Transacao comId(Long id, Transacao transacao) {
        transacao.setId(id);
        return transacao;
    }
}
