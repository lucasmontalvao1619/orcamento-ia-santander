package com.lucdev.orcamentoia.tool;

import com.lucdev.orcamentoia.exception.RecursoNaoEncontradoException;
import com.lucdev.orcamentoia.model.Configuracao;
import com.lucdev.orcamentoia.model.TipoTransacao;
import com.lucdev.orcamentoia.model.Transacao;
import com.lucdev.orcamentoia.service.ConfiguracaoService;
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

    @Mock
    private ConfiguracaoService configuracaoService;

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

    @Test
    void definirSalarioConfirmaOValorEODiaDoRecebimento() {
        when(configuracaoService.definirSalario(new BigDecimal("3000.00"), 15))
                .thenReturn(configuracao("3000.00", 15));

        String resposta = financasTools.definirSalario(new BigDecimal("3000.00"), 15);

        assertThat(resposta).contains("3000,00", "dia 15");
    }

    // Sem dia informado a resposta nao pode inventar uma data que o usuario nao deu.
    @Test
    void definirSalarioOmiteODiaQuandoNaoFoiInformado() {
        when(configuracaoService.definirSalario(new BigDecimal("3000.00"), null))
                .thenReturn(configuracao("3000.00", null));

        String resposta = financasTools.definirSalario(new BigDecimal("3000.00"), null);

        assertThat(resposta).contains("3000,00").doesNotContain("dia");
    }

    @Test
    void consultarSalarioAvisaQuandoNadaFoiConfigurado() {
        when(configuracaoService.obter()).thenReturn(new Configuracao());

        assertThat(financasTools.consultarSalario()).contains("Nenhum salario");
    }

    @Test
    void consultarSalarioInformaOValorConfigurado() {
        when(configuracaoService.obter()).thenReturn(configuracao("2500.00", 5));

        assertThat(financasTools.consultarSalario()).contains("2500,00", "dia 5");
    }

    // A resposta descreve o estado final do lancamento, nao o que foi enviado:
    // e por ela que o usuario confirma que a correcao pegou.
    @Test
    void atualizarTransacaoDescreveOLancamentoJaCorrigido() {
        Transacao corrigida = comId(3L, transacao("Uber", "60.00", "transporte", TipoTransacao.DESPESA));
        when(transacaoService.atualizar(eq(3L), any(), eq(new BigDecimal("60.00")), any(), any()))
                .thenReturn(corrigida);

        String resposta = financasTools.atualizarTransacao(
                3L, new BigDecimal("60.00"), null, null, null);

        assertThat(resposta).contains("3", "DESPESA", "60,00", "transporte", "Uber");
    }

    @Test
    void atualizarTransacaoPropagaIdInexistente() {
        when(transacaoService.atualizar(eq(99L), any(), any(), any(), any()))
                .thenThrow(new RecursoNaoEncontradoException("Nao existe transacao com o id 99."));

        assertThatThrownBy(() -> financasTools.atualizarTransacao(99L, new BigDecimal("10.00"), null, null, null))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void apagarTransacaoConfirmaOQueFoiRemovido() {
        when(transacaoService.apagar(4L))
                .thenReturn(comId(4L, transacao("Cinema", "45.00", "lazer", TipoTransacao.DESPESA)));

        String resposta = financasTools.apagarTransacao(4L);

        assertThat(resposta).contains("DESPESA", "45,00", "lazer");
    }

    @Test
    void apagarTransacaoPropagaIdInexistente() {
        when(transacaoService.apagar(99L))
                .thenThrow(new RecursoNaoEncontradoException("Nao existe transacao com o id 99."));

        assertThatThrownBy(() -> financasTools.apagarTransacao(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    private Configuracao configuracao(String salario, Integer diaRecebimento) {
        Configuracao configuracao = new Configuracao();
        configuracao.setSalario(new BigDecimal(salario));
        configuracao.setDiaRecebimento(diaRecebimento);
        return configuracao;
    }

    private Transacao transacao(String descricao, String valor, String categoria, TipoTransacao tipo) {
        return new Transacao(descricao, new BigDecimal(valor), categoria, tipo);
    }

    private Transacao comId(Long id, Transacao transacao) {
        transacao.setId(id);
        return transacao;
    }
}
