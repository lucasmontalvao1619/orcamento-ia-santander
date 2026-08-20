package com.lucdev.orcamentoia.service;

import com.lucdev.orcamentoia.model.Configuracao;
import com.lucdev.orcamentoia.model.TipoTransacao;
import com.lucdev.orcamentoia.model.Transacao;
import com.lucdev.orcamentoia.repository.ConfiguracaoRepository;
import com.lucdev.orcamentoia.repository.TransacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfiguracaoServiceTest {

    @Mock
    private ConfiguracaoRepository repository;

    @Mock
    private TransacaoRepository transacaoRepository;

    @InjectMocks
    private ConfiguracaoService service;

    private Transacao salarioSalvo;

    @BeforeEach
    void preparar() {
        salarioSalvo = new Transacao("Salário", new BigDecimal("4500.00"), "salario", TipoTransacao.RECEITA);
        salarioSalvo.setId(7L);
    }

    @Test
    void noPrimeiroAcessoNaoHaSalarioConfigurado() {
        when(repository.findAll()).thenReturn(List.of());

        Configuracao configuracao = service.obter();

        assertThat(configuracao.isConfigurado()).isFalse();
        assertThat(configuracao.getSalario()).isNull();
    }

    @Test
    void definirSalarioPelaPrimeiraVezCriaAReceitaCorrespondente() {
        when(repository.findAll()).thenReturn(List.of());
        when(transacaoRepository.save(any(Transacao.class))).thenReturn(salarioSalvo);
        when(repository.save(any(Configuracao.class))).thenAnswer(i -> i.getArgument(0));

        service.definirSalario(new BigDecimal("4500.00"));

        ArgumentCaptor<Transacao> captor = ArgumentCaptor.forClass(Transacao.class);
        verify(transacaoRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoTransacao.RECEITA);
        assertThat(captor.getValue().getCategoria()).isEqualTo("salario");
        assertThat(captor.getValue().getValor()).isEqualByComparingTo("4500.00");
    }

    // Esta e a regra que evita o salario ser contado duas vezes no saldo.
    @Test
    void alterarOSalarioAtualizaAReceitaExistenteEmVezDeCriarOutra() {
        Configuracao existente = new Configuracao();
        existente.setSalario(new BigDecimal("4500.00"));
        existente.setTransacaoSalarioId(7L);

        when(repository.findAll()).thenReturn(List.of(existente));
        when(transacaoRepository.findById(7L)).thenReturn(Optional.of(salarioSalvo));
        when(transacaoRepository.save(any(Transacao.class))).thenAnswer(i -> i.getArgument(0));
        when(repository.save(any(Configuracao.class))).thenAnswer(i -> i.getArgument(0));

        service.definirSalario(new BigDecimal("5000.00"));

        // A mesma transacao (id 7) foi reaproveitada, com o valor novo.
        ArgumentCaptor<Transacao> captor = ArgumentCaptor.forClass(Transacao.class);
        verify(transacaoRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(7L);
        assertThat(captor.getValue().getValor()).isEqualByComparingTo("5000.00");
    }

    @Test
    void recriaAReceitaQuandoATransacaoDeSalarioFoiApagadaPorFora() {
        Configuracao existente = new Configuracao();
        existente.setSalario(new BigDecimal("4500.00"));
        existente.setTransacaoSalarioId(99L);

        when(repository.findAll()).thenReturn(List.of(existente));
        when(transacaoRepository.findById(99L)).thenReturn(Optional.empty());
        when(transacaoRepository.save(any(Transacao.class))).thenReturn(salarioSalvo);
        when(repository.save(any(Configuracao.class))).thenAnswer(i -> i.getArgument(0));

        service.definirSalario(new BigDecimal("3000.00"));

        ArgumentCaptor<Transacao> captor = ArgumentCaptor.forClass(Transacao.class);
        verify(transacaoRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
    }

    @Test
    void salarioNaoPositivoERejeitado() {
        assertThatThrownBy(() -> service.definirSalario(new BigDecimal("-100")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positivo");

        assertThatThrownBy(() -> service.definirSalario(null))
                .isInstanceOf(IllegalArgumentException.class);

        verify(transacaoRepository, never()).save(any());
    }
}
