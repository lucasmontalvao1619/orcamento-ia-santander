package com.lucdev.orcamentoia.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// O aviso de lentidao depende disto. Se der falso negativo, a pessoa espera
// dois minutos achando que travou; falso positivo, e o app instalado mostra um
// aviso que nao se aplica.
class AmbienteDeExecucaoTest {

    @Test
    void emContainerQuandoDetectado() {
        assertThat(new AmbienteDeExecucao(true).isEmContainer()).isTrue();
    }

    @Test
    void foraDeContainerQuandoNaoDetectado() {
        assertThat(new AmbienteDeExecucao(false).isEmContainer()).isFalse();
    }

    // A suite roda fora de container; se este teste falhar, a deteccao esta
    // acusando container onde nao ha.
    @Test
    void aDeteccaoRealNaoAcusaContainerNaMaquinaDeDesenvolvimento() {
        assertThat(new AmbienteDeExecucao().isEmContainer()).isFalse();
    }
}
