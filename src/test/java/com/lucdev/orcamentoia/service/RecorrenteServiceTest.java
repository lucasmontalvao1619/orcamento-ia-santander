package com.lucdev.orcamentoia.service;

import com.lucdev.orcamentoia.exception.RecursoNaoEncontradoException;
import com.lucdev.orcamentoia.model.Recorrente;
import com.lucdev.orcamentoia.model.TipoTransacao;
import com.lucdev.orcamentoia.repository.RecorrenteRepository;
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

        servico.lancar(1L, new BigDecimal("143.27"));

        verify(transacaoService).registrar(eq("Conta de luz"), eq(new BigDecimal("143.27")),
                eq("moradia"), eq(TipoTransacao.DESPESA));
    }

    @Test
    void semValorInformadoUsaAPrevisao() {
        Recorrente net = new Recorrente("Internet", "moradia", TipoTransacao.DESPESA,
                new BigDecimal("120.00"), 10);
        when(repository.findById(1L)).thenReturn(Optional.of(net));

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
}
