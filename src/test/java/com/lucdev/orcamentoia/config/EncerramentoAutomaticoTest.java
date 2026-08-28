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

    // Rede de seguranca para quando o aviso de fechamento nao chega: navegador
    // encerrado a forca, queda de energia. O prazo e longo porque o caminho
    // normal de fechar e o aviso, nao este.
    @Test
    void encerraQuandoOsSinaisParamDeVezPorMuitoTempo() {
        long agora = INICIO + Duration.ofHours(1).toMillis();
        long ultimoSinal = agora - Duration.ofMinutes(15).toMillis();

        assertThat(EncerramentoAutomatico.deveEncerrar(agora, ultimoSinal, INICIO)).isTrue();
    }

    // Enquanto o navegador ainda esta abrindo, nenhum sinal chegou — e encerrar
    // ali mataria a aplicacao antes de o usuario ver a tela.
    @Test
    void esperaAJanelaAbrirAntesDeDesistir() {
        long agora = INICIO + Duration.ofSeconds(30).toMillis();

        assertThat(EncerramentoAutomatico.deveEncerrar(agora, 0, INICIO)).isFalse();
    }

    // Sem nenhum sinal, NUNCA encerrar. Uma pagina antiga em cache do navegador
    // nao envia sinal: com a regra anterior, o app se matava sozinho depois de
    // alguns minutos e todo comando virava "nao foi possivel falar com o
    // servidor" — aconteceu em uso real.
    @Test
    void semSinalNenhumJamaisEncerra() {
        assertThat(EncerramentoAutomatico.deveEncerrar(INICIO + Duration.ofMinutes(5).toMillis(), 0, INICIO)).isFalse();
        assertThat(EncerramentoAutomatico.deveEncerrar(INICIO + Duration.ofHours(3).toMillis(), 0, INICIO)).isFalse();
    }

    @Test
    void desligadoNaoEncerraNemSemSinalNenhum() {
        EncerramentoAutomatico desligado = new EncerramentoAutomatico(false, null);

        // Sem excecao e sem efeito: a verificacao sai logo na primeira linha.
        desligado.verificar();

        assertThat(desligado.isHabilitado()).isFalse();
    }

    // --- aviso de fechamento -------------------------------------------------

    // O X da janela precisa encerrar depressa, sem esperar o prazo longo do
    // sinal de vida.
    @Test
    void avisoDeFechamentoEncerraDepoisDaEspera() {
        long aviso = INICIO;
        long depois = aviso + Duration.ofSeconds(10).toMillis();

        assertThat(EncerramentoAutomatico.avisoDeFechamentoExpirou(depois, aviso)).isTrue();
    }

    // Recarregar a pagina dispara o mesmo aviso. Encerrar na hora fecharia o
    // aplicativo a cada F5.
    @Test
    void logoAposOAvisoAindaNaoEncerra() {
        long aviso = INICIO;
        long logoDepois = aviso + Duration.ofSeconds(3).toMillis();

        assertThat(EncerramentoAutomatico.avisoDeFechamentoExpirou(logoDepois, aviso)).isFalse();
    }

    @Test
    void semAvisoNaoEncerraPorEssaVia() {
        assertThat(EncerramentoAutomatico.avisoDeFechamentoExpirou(
                INICIO + Duration.ofHours(1).toMillis(), 0)).isFalse();
    }

    // O caso que quebrou em uso real: trocar de aba congela o temporizador do
    // navegador e os sinais param. Com tolerancia curta, a aplicacao morria com
    // o usuario ainda usando.
    @Test
    void trocarDeAbaPorMinutosNaoDerrubaAAplicacao() {
        long agora = INICIO + Duration.ofMinutes(30).toMillis();
        long ultimoSinal = agora - Duration.ofMinutes(4).toMillis();

        assertThat(EncerramentoAutomatico.deveEncerrar(agora, ultimoSinal, INICIO)).isFalse();
    }

    @Test
    void sinalCancelaOAvisoDeFechamento() {
        EncerramentoAutomatico e = new EncerramentoAutomatico(true, null);
        e.registrarAvisoDeFechamento();
        e.registrarSinalDeVida();

        // Sem excecao e sem encerramento: o aviso foi cancelado pela recarga.
        e.verificar();
    }
}
