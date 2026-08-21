package com.lucdev.orcamentoia.service;

import com.lucdev.orcamentoia.model.TipoTransacao;
import com.lucdev.orcamentoia.tool.AppTools;
import com.lucdev.orcamentoia.tool.FinancasTools;
import com.lucdev.orcamentoia.tool.InvestimentoTools;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// O interpretador e o que faz o aplicativo funcionar sem chave e sem modelo
// baixado. Como e codigo de regras, cada frase precisa ter um teste: um ajuste
// numa regra quebra outra em silencio.
@ExtendWith(MockitoExtension.class)
class InterpretadorDeComandosTest {

    @Mock
    private FinancasTools financas;

    @Mock
    private InvestimentoTools investimentos;

    @Mock
    private AppTools app;

    @InjectMocks
    private InterpretadorDeComandos interpretador;

    @Test
    void registraDespesaInferindoACategoria() {
        when(financas.registrarTransacao(any(), any(), any(), any())).thenReturn("ok");

        assertThat(interpretador.interpretar("gastei 60 no uber")).contains("ok");

        ArgumentCaptor<String> descricao = ArgumentCaptor.forClass(String.class);
        verify(financas).registrarTransacao(descricao.capture(), eq(new BigDecimal("60")),
                eq("transporte"), eq(TipoTransacao.DESPESA));
        assertThat(descricao.getValue()).isEqualToIgnoringCase("Uber");
    }

    // Acento nao pode mudar o resultado: quem digita "almoço" e quem digita
    // "almoco" espera a mesma coisa.
    @Test
    void acentoNaoAtrapalhaACategoria() {
        when(financas.registrarTransacao(any(), any(), any(), any())).thenReturn("ok");

        interpretador.interpretar("gastei 32 no almoço");

        verify(financas).registrarTransacao(any(), eq(new BigDecimal("32")),
                eq("alimentacao"), eq(TipoTransacao.DESPESA));
    }

    @Test
    void entendeValorComCentavosNoFormatoBrasileiro() {
        when(financas.registrarTransacao(any(), any(), any(), any())).thenReturn("ok");

        interpretador.interpretar("paguei 1.250,90 de aluguel");

        verify(financas).registrarTransacao(any(), eq(new BigDecimal("1250.90")),
                eq("moradia"), eq(TipoTransacao.DESPESA));
    }

    @Test
    void distingueReceitaDeDespesa() {
        when(financas.registrarTransacao(any(), any(), any(), any())).thenReturn("ok");

        interpretador.interpretar("recebi 500 de freela");

        verify(financas).registrarTransacao(any(), eq(new BigDecimal("500")),
                eq("extra"), eq(TipoTransacao.RECEITA));
    }

    @Test
    void defineSalarioComDia() {
        when(financas.definirSalario(any(), any())).thenReturn("ok");

        interpretador.interpretar("meu salario e 3000, dia 15");

        verify(financas).definirSalario(eq(new BigDecimal("3000")), eq(15));
    }

    @Test
    void consultaSaldo() {
        when(financas.consultarSaldo()).thenReturn("saldo");

        assertThat(interpretador.interpretar("qual e o meu saldo")).contains("saldo");
    }

    @Test
    void consultaGastoPorCategoria() {
        when(financas.consultarGastoPorCategoria("transporte")).thenReturn("total");

        assertThat(interpretador.interpretar("quanto gastei com transporte")).contains("total");
    }

    // O valor vem depois do "para": sem isso o id 3 seria lido como valor.
    @Test
    void corrigeUsandoOValorDepoisDoPara() {
        when(financas.atualizarTransacao(any(), any(), any(), any(), any())).thenReturn("ok");

        interpretador.interpretar("corrige a transacao 3 para 45 reais");

        verify(financas).atualizarTransacao(eq(3L), eq(new BigDecimal("45")), any(), any(), any());
    }

    @Test
    void apagaTransacaoPeloId() {
        when(financas.apagarTransacao(3L)).thenReturn("apagada");

        assertThat(interpretador.interpretar("apaga a transacao 3")).contains("apagada");
    }

    // "apagar movimento" e do porquinho, nao das transacoes: sem a ordem certa,
    // apagar um aporte apagaria um lancamento do orcamento.
    @Test
    void apagarMovimentoVaiParaOPorquinhoENaoParaAsTransacoes() {
        when(investimentos.apagarMovimentoDoPorquinho(2L)).thenReturn("movimento apagado");

        assertThat(interpretador.interpretar("apaga o movimento 2")).contains("movimento apagado");
        verifyNoInteractions(financas);
    }

    @Test
    void guardaNoPorquinho() {
        when(investimentos.guardarNoPorquinho(any(), any())).thenReturn("guardado");

        assertThat(interpretador.interpretar("guarda 200 no porquinho")).contains("guardado");
        verify(investimentos).guardarNoPorquinho(any(), eq(new BigDecimal("200")));
    }

    @Test
    void retiraDoPorquinho() {
        when(investimentos.retirarDoPorquinho(any(), any())).thenReturn("retirado");

        assertThat(interpretador.interpretar("tira 100 do porquinho")).contains("retirado");
    }

    @Test
    void consultaOPorquinho() {
        when(investimentos.consultarPorquinho()).thenReturn("total guardado");

        assertThat(interpretador.interpretar("quanto eu tenho guardado")).contains("total guardado");
    }

    @Test
    void listaTransacoes() {
        when(financas.listarTransacoes()).thenReturn("lista");

        assertThat(interpretador.interpretar("listar transacoes")).contains("lista");
    }

    @Test
    void respondeQuemFezOApp() {
        when(app.consultarAutor()).thenReturn("Lucas");

        assertThat(interpretador.interpretar("quem fez esse app")).contains("Lucas");
    }

    // Nao entender precisa ser explicito: inventar um lancamento a partir de
    // uma frase incompreendida seria pior do que admitir que nao entendeu.
    @Test
    void naoInventaQuandoNaoEntende() {
        assertThat(interpretador.interpretar("qual a previsao do tempo amanha")).isEmpty();
        verifyNoInteractions(financas, investimentos, app);
    }

    @Test
    void comandoVazioNaoFazNada() {
        assertThat(interpretador.interpretar("   ")).isEmpty();
        assertThat(interpretador.interpretar(null)).isEmpty();
    }

    // --- repertorio ampliado -------------------------------------------------

    @Test
    void respondeSaudacaoSemMexerEmDinheiro() {
        assertThat(interpretador.interpretar("ola")).isPresent();
        assertThat(interpretador.interpretar("bom dia")).isPresent();
        verifyNoInteractions(financas, investimentos);
    }

    @Test
    void pedidoDeAjudaListaOsComandos() {
        assertThat(interpretador.interpretar("me ajuda")).get().asString().contains("gastei 60 no uber");
        assertThat(interpretador.interpretar("quais os comandos")).isPresent();
    }

    @Test
    void agradecimentoEDespedidaTemResposta() {
        assertThat(interpretador.interpretar("obrigado")).isPresent();
        assertThat(interpretador.interpretar("tchau")).isPresent();
    }

    // Recurso que existia so como botao na tela: sem isto, quem nao tem salario
    // nao conseguiria declarar isso pelo assistente.
    @Test
    void declaraQueNaoTemSalarioFixo() {
        when(financas.declararQueNaoTenhoSalario()).thenReturn("ok");

        assertThat(interpretador.interpretar("nao tenho salario fixo")).contains("ok");
        assertThat(interpretador.interpretar("sou autonomo, salario variavel")).contains("ok");
    }

    @Test
    void reconheceMuitasFormasDePerguntarOSaldo() {
        when(financas.consultarSaldo()).thenReturn("saldo");

        for (String frase : new String[]{"qual e o meu saldo", "quanto eu tenho", "como estou",
                "estou no vermelho", "quanto sobrou", "me da um resumo", "posso gastar"}) {
            assertThat(interpretador.interpretar(frase)).as(frase).contains("saldo");
        }
    }

    @Test
    void reconheceMaisVerbosDeDespesa() {
        when(financas.registrarTransacao(any(), any(), any(), any())).thenReturn("ok");

        for (String frase : new String[]{"torrei 40 no cinema", "saiu 40 de netflix",
                "custou 40 o show", "comprei 40 de livro"}) {
            assertThat(interpretador.interpretar(frase)).as(frase).contains("ok");
        }
        verify(financas, org.mockito.Mockito.times(4))
                .registrarTransacao(any(), any(), eq("lazer"), eq(TipoTransacao.DESPESA));
    }

    @Test
    void reconheceMaisVerbosDeReceita() {
        when(financas.registrarTransacao(any(), any(), any(), any())).thenReturn("ok");

        interpretador.interpretar("faturei 800 de comissao");

        verify(financas).registrarTransacao(any(), eq(new BigDecimal("800")),
                eq("extra"), eq(TipoTransacao.RECEITA));
    }

    // "presente" e receita. Se as listas de categoria fossem uma so, cairia em
    // lazer e o lancamento entraria errado.
    @Test
    void presenteEhReceitaENaoLazer() {
        when(financas.registrarTransacao(any(), any(), any(), any())).thenReturn("ok");

        interpretador.interpretar("ganhei 200 de presente");

        verify(financas).registrarTransacao(any(), eq(new BigDecimal("200")),
                eq("presente"), eq(TipoTransacao.RECEITA));
    }

    @Test
    void cobreCategoriasNovasDeDespesa() {
        when(financas.registrarTransacao(any(), any(), any(), any())).thenReturn("ok");

        interpretador.interpretar("paguei 90 na farmacia");
        verify(financas).registrarTransacao(any(), any(), eq("saude"), any());

        interpretador.interpretar("gastei 55 de gasolina");
        verify(financas).registrarTransacao(any(), any(), eq("transporte"), any());

        interpretador.interpretar("paguei 120 de internet");
        verify(financas).registrarTransacao(any(), any(), eq("moradia"), any());
    }

    @Test
    void entendeGastosComCategoriaSemAPalavraQuanto() {
        when(financas.consultarGastoPorCategoria("saude")).thenReturn("total");

        assertThat(interpretador.interpretar("gastos com farmacia")).contains("total");
    }

    // Bug real: "gas" (moradia) casava dentro de "gastos", e "gastos com
    // farmacia" virava moradia. Palavras curtas colidem com palavras maiores o
    // tempo todo — este teste prende o casamento por palavra inteira.
    @Test
    void palavraCurtaNaoCasaDentroDeOutraPalavra() {
        when(financas.consultarGastoPorCategoria("saude")).thenReturn("total");

        assertThat(interpretador.interpretar("gastos com farmacia")).contains("total");
    }

    @Test
    void naoConfundeCategoriaComPedacoDePalavra() {
        when(financas.registrarTransacao(any(), any(), any(), any())).thenReturn("ok");

        // "moto" nao pode transformar "motorista" em transporte por acidente do
        // texto; aqui a categoria correta vem de outra palavra da frase.
        interpretador.interpretar("paguei 30 de lanche");

        verify(financas).registrarTransacao(any(), any(), eq("alimentacao"), any());
    }
}
