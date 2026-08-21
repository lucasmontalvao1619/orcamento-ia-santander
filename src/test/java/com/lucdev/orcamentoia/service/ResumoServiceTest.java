package com.lucdev.orcamentoia.service;

import com.lucdev.orcamentoia.model.TipoTransacao;
import com.lucdev.orcamentoia.model.Transacao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// A tabela por modalidade e o que responde "para onde meu dinheiro esta indo".
// Se ela somar errado, a pessoa toma decisao errada sobre o proprio dinheiro.
@ExtendWith(MockitoExtension.class)
class ResumoServiceTest {

    @Mock
    private TransacaoService transacaoService;

    @InjectMocks
    private ResumoService servico;

    private Transacao t(String desc, String valor, String categoria, TipoTransacao tipo, LocalDateTime quando) {
        Transacao x = new Transacao(desc, new BigDecimal(valor), categoria, tipo);
        x.setDataHora(quando);
        return x;
    }

    @Test
    void somaPorCategoriaEOrdenaPeloMaiorGasto() {
        LocalDateTime agora = LocalDateTime.now();
        when(transacaoService.listarTodas()).thenReturn(List.of(
                t("Uber", "60.00", "transporte", TipoTransacao.DESPESA, agora),
                t("Mercado", "200.00", "alimentacao", TipoTransacao.DESPESA, agora),
                t("Almoco", "50.00", "alimentacao", TipoTransacao.DESPESA, agora),
                t("Salario", "3000.00", "salario", TipoTransacao.RECEITA, agora)));

        ResumoService.Resumo r = servico.montar(true);

        assertThat(r.totalDespesas()).isEqualByComparingTo("310.00");
        assertThat(r.totalReceitas()).isEqualByComparingTo("3000.00");
        assertThat(r.saldo()).isEqualByComparingTo("2690.00");
        // Maior gasto primeiro: e o que a pessoa procura ao abrir a tabela.
        assertThat(r.despesasPorCategoria().get(0).categoria()).isEqualTo("alimentacao");
        assertThat(r.despesasPorCategoria().get(0).total()).isEqualByComparingTo("250.00");
        assertThat(r.despesasPorCategoria().get(0).lancamentos()).isEqualTo(2);
    }

    @Test
    void calculaOPercentualDeCadaModalidade() {
        LocalDateTime agora = LocalDateTime.now();
        when(transacaoService.listarTodas()).thenReturn(List.of(
                t("A", "75.00", "alimentacao", TipoTransacao.DESPESA, agora),
                t("B", "25.00", "transporte", TipoTransacao.DESPESA, agora)));

        ResumoService.Resumo r = servico.montar(true);

        assertThat(r.despesasPorCategoria().get(0).percentual()).isEqualByComparingTo("75.0");
        assertThat(r.despesasPorCategoria().get(1).percentual()).isEqualByComparingTo("25.0");
    }

    // Sem o filtro de mes o resumo mistura periodos e nao serve para decidir
    // nada sobre o mes que esta correndo.
    @Test
    void filtraPeloMesAtualQuandoPedido() {
        when(transacaoService.listarTodas()).thenReturn(List.of(
                t("Deste mes", "100.00", "lazer", TipoTransacao.DESPESA, LocalDateTime.now()),
                t("Ano passado", "999.00", "lazer", TipoTransacao.DESPESA,
                        LocalDateTime.now().minusYears(1))));

        assertThat(servico.montar(true).totalDespesas()).isEqualByComparingTo("100.00");
        assertThat(servico.montar(false).totalDespesas()).isEqualByComparingTo("1099.00");
    }

    // Divisao por zero: sem lancamento nenhum, o percentual nao pode explodir.
    @Test
    void semLancamentosNaoQuebra() {
        when(transacaoService.listarTodas()).thenReturn(List.of());

        ResumoService.Resumo r = servico.montar(true);

        assertThat(r.totalDespesas()).isEqualByComparingTo("0");
        assertThat(r.despesasPorCategoria()).isEmpty();
    }

    @Test
    void categoriaNulaViraOutros() {
        when(transacaoService.listarTodas()).thenReturn(List.of(
                t("Sem categoria", "10.00", null, TipoTransacao.DESPESA, LocalDateTime.now())));

        assertThat(servico.montar(true).despesasPorCategoria().get(0).categoria()).isEqualTo("outros");
    }
}
