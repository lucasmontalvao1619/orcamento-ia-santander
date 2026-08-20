package com.lucdev.orcamentoia.service;

import com.lucdev.orcamentoia.dto.NovaTransacaoRequest;
import com.lucdev.orcamentoia.exception.RecursoNaoEncontradoException;
import com.lucdev.orcamentoia.model.TipoTransacao;
import com.lucdev.orcamentoia.model.Transacao;
import com.lucdev.orcamentoia.repository.TransacaoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransacaoServiceTest {

    @Mock
    private TransacaoRepository repository;

    @InjectMocks
    private TransacaoService transacaoService;

    @Captor
    private ArgumentCaptor<Transacao> transacaoSalva;

    @Test
    void calculaOSaldoComoReceitasMenosDespesas() {
        when(repository.findByTipo(TipoTransacao.RECEITA))
                .thenReturn(List.of(transacao("Salario", "3000.00", TipoTransacao.RECEITA)));
        when(repository.findByTipo(TipoTransacao.DESPESA))
                .thenReturn(List.of(
                        transacao("Mercado", "500.00", TipoTransacao.DESPESA),
                        transacao("Onibus", "9.50", TipoTransacao.DESPESA)));

        assertThat(transacaoService.calcularSaldo()).isEqualByComparingTo("2490.50");
    }

    @Test
    void oSaldoEZeroQuandoNaoHaTransacoes() {
        when(repository.findByTipo(any())).thenReturn(List.of());

        assertThat(transacaoService.calcularSaldo()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void rejeitaValorNaoPositivoSemTocarNoBanco() {
        assertThatThrownBy(() -> transacaoService.registrar(
                "Invalida", new BigDecimal("-10.00"), "erro", TipoTransacao.DESPESA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positivo");

        verify(repository, never()).save(any());
    }

    @Test
    void rejeitaValorNuloSemTocarNoBanco() {
        assertThatThrownBy(() -> transacaoService.registrar("Invalida", null, "erro", TipoTransacao.DESPESA))
                .isInstanceOf(IllegalArgumentException.class);

        verify(repository, never()).save(any());
    }

    // A entrada REST passa pela mesma regra da entrada da IA.
    @Test
    void criarAplicaAMesmaValidacaoDeValorPositivo() {
        NovaTransacaoRequest request = new NovaTransacaoRequest(
                "Invalida", BigDecimal.ZERO, "erro", TipoTransacao.DESPESA);

        assertThatThrownBy(() -> transacaoService.criar(request))
                .isInstanceOf(IllegalArgumentException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void criarPersisteOsDadosRecebidos() {
        NovaTransacaoRequest request = new NovaTransacaoRequest(
                "Almoco", new BigDecimal("50.00"), "alimentacao", TipoTransacao.DESPESA);
        when(repository.save(any(Transacao.class))).thenAnswer(chamada -> chamada.getArgument(0));

        transacaoService.criar(request);

        verify(repository).save(transacaoSalva.capture());
        assertThat(transacaoSalva.getValue().getDescricao()).isEqualTo("Almoco");
        assertThat(transacaoSalva.getValue().getValor()).isEqualByComparingTo("50.00");
        assertThat(transacaoSalva.getValue().getCategoria()).isEqualTo("alimentacao");
        assertThat(transacaoSalva.getValue().getTipo()).isEqualTo(TipoTransacao.DESPESA);
        assertThat(transacaoSalva.getValue().getDataHora()).isNotNull();
    }

    @Test
    void apagarRemoveATransacaoEDevolveOQueFoiRemovido() {
        Transacao existente = transacao("Almoco", "50.00", TipoTransacao.DESPESA);
        when(repository.findById(7L)).thenReturn(java.util.Optional.of(existente));

        Transacao removida = transacaoService.apagar(7L);

        verify(repository).delete(existente);
        assertThat(removida).isSameAs(existente);
    }

    // Apagar um id que nao existe tem de virar 404, nao 500.
    @Test
    void apagarIdInexistenteFalhaComRecursoNaoEncontrado() {
        when(repository.findById(99L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> transacaoService.apagar(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("99");

        verify(repository, never()).delete(any());
    }

    private Transacao transacao(String descricao, String valor, TipoTransacao tipo) {
        return new Transacao(descricao, new BigDecimal(valor), "categoria", tipo);
    }
}
