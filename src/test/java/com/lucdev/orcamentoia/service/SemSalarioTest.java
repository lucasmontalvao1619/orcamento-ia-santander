package com.lucdev.orcamentoia.service;

import com.lucdev.orcamentoia.model.Configuracao;
import com.lucdev.orcamentoia.model.TipoTransacao;
import com.lucdev.orcamentoia.model.Transacao;
import com.lucdev.orcamentoia.repository.ConfiguracaoRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Quem vive de renda variavel nao tem um valor mensal para informar. Antes
// disso, a unica forma de sair da tela de boas-vindas era inventar um salario —
// e o valor inventado entrava no saldo como receita.
@ExtendWith(MockitoExtension.class)
class SemSalarioTest {

    @Mock
    private ConfiguracaoRepository repository;

    @Mock
    private TransacaoRepository transacaoRepository;

    @InjectMocks
    private ConfiguracaoService servico;

    @Test
    void declararQueNaoTemSalarioLiberaOAppSemInformarValor() {
        when(repository.findAll()).thenReturn(List.of());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        Configuracao resultado = servico.declararQueNaoTemSalario();

        assertThat(resultado.isSemSalario()).isTrue();
        assertThat(resultado.getSalario()).isNull();
        // E o que a interface consulta para fechar a tela de boas-vindas.
        assertThat(resultado.isConfigurado()).isTrue();
    }

    // Deixar a receita antiga no orcamento manteria no saldo um dinheiro que a
    // pessoa acabou de dizer que nao recebe.
    @Test
    void apagaAReceitaDeSalarioQueJaExistia() {
        Configuracao existente = new Configuracao();
        existente.setSalario(new BigDecimal("3000.00"));
        existente.setTransacaoSalarioId(7L);
        existente.setDiaRecebimento(15);
        Transacao receita = new Transacao("Salário", new BigDecimal("3000.00"), "salario", TipoTransacao.RECEITA);
        receita.setId(7L);

        when(repository.findAll()).thenReturn(List.of(existente));
        when(transacaoRepository.findById(7L)).thenReturn(Optional.of(receita));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        Configuracao resultado = servico.declararQueNaoTemSalario();

        verify(transacaoRepository).delete(receita);
        assertThat(resultado.getSalario()).isNull();
        assertThat(resultado.getTransacaoSalarioId()).isNull();
        assertThat(resultado.getDiaRecebimento()).isNull();
        assertThat(resultado.isSemSalario()).isTrue();
    }

    @Test
    void semReceitaAnteriorNaoTentaApagarNada() {
        when(repository.findAll()).thenReturn(List.of(new Configuracao()));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        servico.declararQueNaoTemSalario();

        verify(transacaoRepository, never()).delete(any());
    }

    // Mudar de ideia depois precisa funcionar: quem arruma um emprego informa o
    // salario e volta ao fluxo normal.
    @Test
    void informarUmSalarioDepoisDesfazAMarca() {
        Configuracao semSalario = new Configuracao();
        semSalario.setSemSalario(true);

        when(repository.findAll()).thenReturn(List.of(semSalario));
        when(transacaoRepository.save(any())).thenAnswer(i -> {
            Transacao t = i.getArgument(0);
            t.setId(1L);
            return t;
        });
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        Configuracao resultado = servico.definirSalario(new BigDecimal("2500.00"), null);

        assertThat(resultado.isSemSalario()).isFalse();
        assertThat(resultado.getSalario()).isEqualByComparingTo("2500.00");
    }
}
