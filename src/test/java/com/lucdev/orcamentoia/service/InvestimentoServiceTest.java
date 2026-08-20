package com.lucdev.orcamentoia.service;

import com.lucdev.orcamentoia.model.Investimento;
import com.lucdev.orcamentoia.model.TipoInvestimento;
import com.lucdev.orcamentoia.repository.InvestimentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
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

    @InjectMocks
    private InvestimentoService service;

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
        when(repository.findByTipo(TipoInvestimento.APORTE)).thenReturn(List.of(aporte("100")));
        when(repository.findByTipo(TipoInvestimento.RETIRADA)).thenReturn(List.of());

        assertThatThrownBy(() -> service.registrar("saque", new BigDecimal("150"), TipoInvestimento.RETIRADA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mais do que existe");

        verify(repository, never()).save(any());
    }

    @Test
    void permiteRetirarExatamenteOTotalGuardado() {
        when(repository.findByTipo(TipoInvestimento.APORTE)).thenReturn(List.of(aporte("100")));
        when(repository.findByTipo(TipoInvestimento.RETIRADA)).thenReturn(List.of());
        when(repository.save(any(Investimento.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(service.registrar("saque", new BigDecimal("100"), TipoInvestimento.RETIRADA)).isNotNull();
    }

    @Test
    void valorNaoPositivoERejeitado() {
        assertThatThrownBy(() -> service.registrar("x", new BigDecimal("-10"), TipoInvestimento.APORTE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positivo");

        verify(repository, never()).save(any());
    }
}
