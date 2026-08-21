package com.lucdev.orcamentoia.service;

import com.lucdev.orcamentoia.exception.RecursoNaoEncontradoException;
import com.lucdev.orcamentoia.model.Recorrente;
import com.lucdev.orcamentoia.model.TipoTransacao;
import com.lucdev.orcamentoia.model.Transacao;
import com.lucdev.orcamentoia.repository.RecorrenteRepository;
import com.lucdev.orcamentoia.repository.TransacaoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Contas de luz, agua e cartao mudam todo mes. O cadastro guarda a PREVISAO e o
// lancamento guarda o valor real — sem isso, ou o usuario corrigiria o cadastro
// toda vez, ou o orcamento mentiria.
@ExtendWith(MockitoExtension.class)
class RecorrenteServiceTest {

    @Mock
    private RecorrenteRepository repository;

    @Mock
    private TransacaoService transacaoService;

    @Mock
    private TransacaoRepository transacaoRepository;

    @InjectMocks
    private RecorrenteService servico;

    @Test
    void cadastraGastoFixoComPrevisao() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        Recorrente r = servico.criar("Internet", "moradia", TipoTransacao.DESPESA,
                new BigDecimal("120.00"), 10);

        assertThat(r.getDescricao()).isEqualTo("Internet");
        assertThat(r.getValorPrevisto()).isEqualByComparingTo("120.00");
        assertThat(r.getDiaVencimento()).isEqualTo(10);
    }

    // Cartao de credito nao tem valor conhecido de antemao: exigir previsao
    // obrigaria a inventar um numero.
    @Test
    void aceitaContaSemPrevisao() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        Recorrente r = servico.criar("Cartao de credito", "outros", TipoTransacao.DESPESA, null, 15);

        assertThat(r.getValorPrevisto()).isNull();
    }

    @Test
    void recusaPrevisaoNegativaEDiaInvalido() {
        assertThatThrownBy(() -> servico.criar("Luz", "moradia", TipoTransacao.DESPESA,
                new BigDecimal("-1"), 5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servico.criar("Luz", "moradia", TipoTransacao.DESPESA,
                new BigDecimal("100"), 32)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servico.criar("  ", "moradia", TipoTransacao.DESPESA,
                null, 5)).isInstanceOf(IllegalArgumentException.class);
    }

    // O caso central da feature: previsto 120, veio 143.
    @Test
    void lancarUsaOValorRealENaoAPrevisao() {
        Recorrente luz = new Recorrente("Conta de luz", "moradia", TipoTransacao.DESPESA,
                new BigDecimal("120.00"), 10);
        when(repository.findById(1L)).thenReturn(Optional.of(luz));
        // lancar guarda o vinculo com o item fixo antes de devolver.
        when(transacaoService.registrar(any(), any(), any(), any()))
                .thenAnswer(i -> new Transacao("Conta de luz", i.getArgument(1), "moradia", TipoTransacao.DESPESA));
        when(transacaoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        servico.lancar(1L, new BigDecimal("143.27"));

        verify(transacaoService).registrar(eq("Conta de luz"), eq(new BigDecimal("143.27")),
                eq("moradia"), eq(TipoTransacao.DESPESA));
    }

    @Test
    void semValorInformadoUsaAPrevisao() {
        Recorrente net = new Recorrente("Internet", "moradia", TipoTransacao.DESPESA,
                new BigDecimal("120.00"), 10);
        when(repository.findById(1L)).thenReturn(Optional.of(net));
        when(transacaoService.registrar(any(), any(), any(), any()))
                .thenAnswer(i -> new Transacao("Internet", i.getArgument(1), "moradia", TipoTransacao.DESPESA));
        when(transacaoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        servico.lancar(1L, null);

        verify(transacaoService).registrar(any(), eq(new BigDecimal("120.00")), any(), any());
    }

    // Sem valor e sem previsao, adivinhar poluiria o saldo com dinheiro
    // inventado. Melhor recusar e dizer o que falta.
    @Test
    void semValorESemPrevisaoRecusaEDizQualConta() {
        Recorrente cartao = new Recorrente("Cartao de credito", "outros", TipoTransacao.DESPESA, null, 15);
        when(repository.findById(1L)).thenReturn(Optional.of(cartao));

        assertThatThrownBy(() -> servico.lancar(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cartao de credito");
    }

    @Test
    void idInexistenteEhRecusado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.lancar(99L, new BigDecimal("10")))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // Itens sem previsao ficam de fora do total: somar zero daria a falsa
    // sensacao de que o mes esta mais barato do que sera.
    @Test
    void totalPrevistoIgnoraItensSemPrevisao() {
        when(repository.findAllByTipoOrderByDiaVencimentoAsc(TipoTransacao.DESPESA)).thenReturn(List.of(
                new Recorrente("Internet", "moradia", TipoTransacao.DESPESA, new BigDecimal("120.00"), 10),
                new Recorrente("Cartao", "outros", TipoTransacao.DESPESA, null, 15),
                new Recorrente("Agua", "moradia", TipoTransacao.DESPESA, new BigDecimal("80.00"), 20)));

        assertThat(servico.totalPrevisto(TipoTransacao.DESPESA)).isEqualByComparingTo("200.00");
        assertThat(servico.quantidadeSemPrevisao(TipoTransacao.DESPESA)).isEqualTo(1);
    }

    // --- fechar o mes --------------------------------------------------------

    private Transacao lancamento(Long recorrenteId, String valor, java.time.LocalDateTime quando) {
        Transacao t = new Transacao("x", new BigDecimal(valor), "moradia", TipoTransacao.DESPESA);
        t.setRecorrenteId(recorrenteId);
        t.setDataHora(quando);
        return t;
    }

    // Clicar duas vezes em "fechar o mes" nao pode dobrar a conta de luz — e o
    // usuario so descobriria olhando o saldo errado depois.
    @Test
    void fecharMesPulaOQueJaFoiLancadoNesteMes() {
        Recorrente luz = new Recorrente("Luz", "moradia", TipoTransacao.DESPESA, new BigDecimal("120"), 10);
        luz.setId(1L);
        when(transacaoRepository.findByRecorrenteIdIsNotNull())
                .thenReturn(List.of(lancamento(1L, "143.00", java.time.LocalDateTime.now())));

        List<Transacao> lancados = servico.fecharMes(java.util.Map.of(1L, new BigDecimal("120")));

        assertThat(lancados).isEmpty();
        verify(transacaoService, org.mockito.Mockito.never()).registrar(any(), any(), any(), any());
    }

    // Lancamento do mes passado nao conta: a conta deste mes precisa ser paga.
    @Test
    void lancamentoDeOutroMesNaoBloqueia() {
        Recorrente luz = new Recorrente("Luz", "moradia", TipoTransacao.DESPESA, new BigDecimal("120"), 10);
        luz.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(luz));
        when(transacaoRepository.findByRecorrenteIdIsNotNull())
                .thenReturn(List.of(lancamento(1L, "110.00", java.time.LocalDateTime.now().minusMonths(2))));
        when(transacaoService.registrar(any(), any(), any(), any()))
                .thenAnswer(i -> new Transacao("Luz", i.getArgument(1), "moradia", TipoTransacao.DESPESA));
        when(transacaoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThat(servico.fecharMes(java.util.Map.of(1L, new BigDecimal("130")))).hasSize(1);
    }

    // O vinculo e por id, nunca por descricao: um lancamento manual chamado
    // "Conta de luz" nao pode passar por conta paga.
    @Test
    void lancamentoDeOutroItemNaoContaComoPago() {
        when(transacaoRepository.findByRecorrenteIdIsNotNull())
                .thenReturn(List.of(lancamento(99L, "143.00", java.time.LocalDateTime.now())));

        assertThat(servico.lancamentoDoMes(1L)).isEmpty();
    }

    @Test
    void lancarGuardaOVinculoComOItemFixo() {
        Recorrente luz = new Recorrente("Luz", "moradia", TipoTransacao.DESPESA, new BigDecimal("120"), 10);
        luz.setId(7L);
        when(repository.findById(7L)).thenReturn(Optional.of(luz));
        when(transacaoService.registrar(any(), any(), any(), any()))
                .thenAnswer(i -> new Transacao("Luz", i.getArgument(1), "moradia", TipoTransacao.DESPESA));
        when(transacaoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Transacao t = servico.lancar(7L, new BigDecimal("143"));

        assertThat(t.getRecorrenteId()).isEqualTo(7L);
    }
}
