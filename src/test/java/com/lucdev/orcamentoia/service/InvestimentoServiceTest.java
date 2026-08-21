package com.lucdev.orcamentoia.service;

import com.lucdev.orcamentoia.model.Investimento;
import com.lucdev.orcamentoia.model.TipoInvestimento;
import com.lucdev.orcamentoia.repository.InvestimentoRepository;
import com.lucdev.orcamentoia.repository.TransacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvestimentoServiceTest {

    @Mock
    private InvestimentoRepository repository;

    // Guardar e retirar movem dinheiro no orcamento: cada movimento gera um
    // lancamento espelho.
    @Mock
    private TransacaoRepository transacaoRepository;

    private InvestimentoService service;

    // 10,65% ao ano, a mesma taxa default da aplicacao.
    private static final BigDecimal CDI = new BigDecimal("0.1065");

    @BeforeEach
    void criarServico() {
        service = new InvestimentoService(repository, CDI, transacaoRepository);
    }

    private Investimento comData(Investimento investimento, LocalDateTime data) {
        investimento.setDataHora(data);
        return investimento;
    }

    private Investimento aporte(String valor) {
        return new Investimento("guardado", new BigDecimal(valor), TipoInvestimento.APORTE);
    }

    private Investimento retirada(String valor) {
        return new Investimento("retirado", new BigDecimal(valor), TipoInvestimento.RETIRADA);
    }

    @Test
    void oTotalEAsomaDosAportesMenosAsRetiradas() {
        when(repository.findByTipo(TipoInvestimento.APORTE)).thenReturn(List.of(aporte("300"), aporte("200")));
        when(repository.findByTipo(TipoInvestimento.RETIRADA)).thenReturn(List.of(retirada("120")));

        assertThat(service.calcularTotal()).isEqualByComparingTo("380");
    }

    @Test
    void oPorquinhoComecaVazio() {
        when(repository.findByTipo(any())).thenReturn(List.of());

        assertThat(service.calcularTotal()).isEqualByComparingTo("0");
    }

    @Test
    void guardarValorRegistraUmAporte() {
        when(repository.save(any(Investimento.class))).thenAnswer(i -> i.getArgument(0));

        Investimento salvo = service.registrar("viagem", new BigDecimal("150.00"), TipoInvestimento.APORTE);

        assertThat(salvo.getTipo()).isEqualTo(TipoInvestimento.APORTE);
        assertThat(salvo.getValor()).isEqualByComparingTo("150.00");
    }

    // O porquinho nunca pode ficar negativo: nao da para tirar dinheiro que
    // nao foi guardado.
    @Test
    void naoPermiteRetirarMaisDoQueExisteGuardado() {
        when(repository.findAll()).thenReturn(List.of(comData(aporte("100"), LocalDateTime.now())));

        assertThatThrownBy(() -> service.registrar("saque", new BigDecimal("150"), TipoInvestimento.RETIRADA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mais do que existe");

        verify(repository, never()).save(any());
    }

    @Test
    void permiteRetirarExatamenteOSaldoDisponivel() {
        when(repository.findAll()).thenReturn(List.of(comData(aporte("100"), LocalDateTime.now())));
        when(repository.save(any(Investimento.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(service.registrar("saque", new BigDecimal("100"), TipoInvestimento.RETIRADA)).isNotNull();
    }

    // ---------------------------------------------------- rendimento (CDI) ----

    @Test
    void semMovimentoNaoHaSaldoNemRendimento() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.calcularSaldoComRendimento()).isEqualByComparingTo("0");
    }

    @Test
    void aporteFeitoAgoraAindaNaoRendeu() {
        when(repository.findAll()).thenReturn(List.of(comData(aporte("1000"), LocalDateTime.now())));

        assertThat(service.calcularSaldoComRendimento()).isEqualByComparingTo("1000");
    }

    // 1000 reais a 10,65% ao ano, apos um ano corrido, tem de ficar perto de
    // 1106,50 - nao exato, porque a contagem e em dias uteis.
    @Test
    void aporteDeUmAnoAtrasRendeuPertoDaTaxaAnual() {
        when(repository.findAll()).thenReturn(
                List.of(comData(aporte("1000"), LocalDateTime.now().minusYears(1))));

        assertThat(service.calcularSaldoComRendimento().doubleValue())
                .isBetween(1100.0, 1113.0);
    }

    @Test
    void oRendimentoEADiferencaEntreOSaldoCorrigidoEOqueFoiDepositado() {
        when(repository.findAll()).thenReturn(
                List.of(comData(aporte("1000"), LocalDateTime.now().minusYears(1))));
        when(repository.findByTipo(TipoInvestimento.APORTE)).thenReturn(List.of(aporte("1000")));
        when(repository.findByTipo(TipoInvestimento.RETIRADA)).thenReturn(List.of());

        assertThat(service.calcularRendimento().doubleValue()).isBetween(100.0, 113.0);
    }

    // O que rendeu passa a render tambem: dois anos precisam superar o dobro
    // de um ano so.
    @Test
    void oRendimentoEComposto() {
        when(repository.findAll()).thenReturn(
                List.of(comData(aporte("1000"), LocalDateTime.now().minusYears(2))));

        assertThat(service.calcularSaldoComRendimento().doubleValue()).isGreaterThan(1213.0);
    }

    @Test
    void valorNaoPositivoERejeitado() {
        assertThatThrownBy(() -> service.registrar("x", new BigDecimal("-10"), TipoInvestimento.APORTE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positivo");

        verify(repository, never()).save(any());
    }

    // --- o dinheiro precisa se mover no orcamento ---------------------------

    // Guardar tira da conta. Sem isso, guardar nao custaria nada e o dinheiro
    // apareceria em dois lugares ao mesmo tempo.
    @Test
    void guardarGeraUmaSaidaNoOrcamento() {
        when(repository.save(any())).thenAnswer(i -> {
            com.lucdev.orcamentoia.model.Investimento inv = i.getArgument(0);
            inv.setId(1L);
            return inv;
        });

        service.registrar("Viagem", new BigDecimal("200.00"), TipoInvestimento.APORTE);

        org.mockito.ArgumentCaptor<com.lucdev.orcamentoia.model.Transacao> captor =
                org.mockito.ArgumentCaptor.forClass(com.lucdev.orcamentoia.model.Transacao.class);
        verify(transacaoRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo())
                .isEqualTo(com.lucdev.orcamentoia.model.TipoTransacao.DESPESA);
        assertThat(captor.getValue().getValor()).isEqualByComparingTo("200.00");
        assertThat(captor.getValue().getInvestimentoId()).isEqualTo(1L);
    }

    // O pedido original: retirar devolve o dinheiro para a conta.
    @Test
    void retirarDevolveODinheiroParaAConta() {
        when(repository.findAll()).thenReturn(List.of(
                comData(aporte("500.00"), LocalDateTime.now().minusDays(1))));
        when(repository.save(any())).thenAnswer(i -> {
            com.lucdev.orcamentoia.model.Investimento inv = i.getArgument(0);
            inv.setId(2L);
            return inv;
        });

        service.registrar("Emergencia", new BigDecimal("100.00"), TipoInvestimento.RETIRADA);

        org.mockito.ArgumentCaptor<com.lucdev.orcamentoia.model.Transacao> captor =
                org.mockito.ArgumentCaptor.forClass(com.lucdev.orcamentoia.model.Transacao.class);
        verify(transacaoRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo())
                .isEqualTo(com.lucdev.orcamentoia.model.TipoTransacao.RECEITA);
        assertThat(captor.getValue().getValor()).isEqualByComparingTo("100.00");
    }

    // Apagar o movimento apaga o espelho: deixa-lo faria o saldo continuar
    // descontando um dinheiro que voltou para a conta.
    @Test
    void apagarOMovimentoApagaOLancamentoEspelho() {
        com.lucdev.orcamentoia.model.Investimento inv =
                comData(aporte("100.00"), LocalDateTime.now());
        inv.setId(7L);
        com.lucdev.orcamentoia.model.Transacao espelho =
                new com.lucdev.orcamentoia.model.Transacao("Guardado", new BigDecimal("100.00"),
                        InvestimentoService.CATEGORIA_PORQUINHO,
                        com.lucdev.orcamentoia.model.TipoTransacao.DESPESA);
        when(repository.findById(7L)).thenReturn(java.util.Optional.of(inv));
        when(transacaoRepository.findByInvestimentoId(7L)).thenReturn(java.util.Optional.of(espelho));

        service.apagar(7L);

        verify(transacaoRepository).delete(espelho);
    }

    @Test
    void oLancamentoEspelhoUsaCategoriaPropria() {
        when(repository.save(any())).thenAnswer(i -> {
            com.lucdev.orcamentoia.model.Investimento inv = i.getArgument(0);
            inv.setId(1L);
            return inv;
        });

        service.registrar("Reserva", new BigDecimal("50.00"), TipoInvestimento.APORTE);

        org.mockito.ArgumentCaptor<com.lucdev.orcamentoia.model.Transacao> captor =
                org.mockito.ArgumentCaptor.forClass(com.lucdev.orcamentoia.model.Transacao.class);
        verify(transacaoRepository).save(captor.capture());
        // Guardar dinheiro nao e "gasto com lazer": categoria propria deixa o
        // resumo por modalidade honesto.
        assertThat(captor.getValue().getCategoria()).isEqualTo(InvestimentoService.CATEGORIA_PORQUINHO);
    }
}
