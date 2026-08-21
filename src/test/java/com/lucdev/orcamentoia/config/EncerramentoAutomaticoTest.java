package com.lucdev.orcamentoia.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

// A decisao aqui derruba o processo. Errar para mais deixa a aplicacao rodando
// invisivel depois de fechada; errar para menos fecha o app na cara do usuario
// no meio do uso — um F5 basta.
class EncerramentoAutomaticoTest {

    private static final long INICIO = 1_000_000L;

    @Test
    void naoEncerraEnquantoAJanelaMandaSinal() {
        long agora = INICIO + Duration.ofMinutes(30).toMillis();
        long ultimoSinal = agora - 4000;

        assertThat(EncerramentoAutomatico.deveEncerrar(agora, ultimoSinal, INICIO)).isFalse();
    }

    // Entre o fechamento e a abertura da pagina nova os sinais param por um
    // instante. Encerrar nessa janela fecharia o app a cada refresh.
    @Test
    void toleraOIntervaloDeUmRefresh() {
        long agora = INICIO + Duration.ofMinutes(10).toMillis();
        long ultimoSinal = agora - Duration.ofSeconds(10).toMillis();

        assertThat(EncerramentoAutomatico.deveEncerrar(agora, ultimoSinal, INICIO)).isFalse();
    }

    @Test
    void encerraQuandoOsSinaisParamDeVez() {
        long agora = INICIO + Duration.ofMinutes(10).toMillis();
        long ultimoSinal = agora - Duration.ofSeconds(20).toMillis();

        assertThat(EncerramentoAutomatico.deveEncerrar(agora, ultimoSinal, INICIO)).isTrue();
    }

    // Enquanto o navegador ainda esta abrindo, nenhum sinal chegou — e encerrar
    // ali mataria a aplicacao antes de o usuario ver a tela.
    @Test
    void esperaAJanelaAbrirAntesDeDesistir() {
        long agora = INICIO + Duration.ofSeconds(30).toMillis();

        assertThat(EncerramentoAutomatico.deveEncerrar(agora, 0, INICIO)).isFalse();
    }

    @Test
    void desisteSeAJanelaNuncaAbrir() {
        long agora = INICIO + Duration.ofMinutes(5).toMillis();

        assertThat(EncerramentoAutomatico.deveEncerrar(agora, 0, INICIO)).isTrue();
    }

    @Test
    void desligadoNaoEncerraNemSemSinalNenhum() {
        EncerramentoAutomatico desligado = new EncerramentoAutomatico(false, null);

        // Sem excecao e sem efeito: a verificacao sai logo na primeira linha.
        desligado.verificar();

        assertThat(desligado.isHabilitado()).isFalse();
    }
}
