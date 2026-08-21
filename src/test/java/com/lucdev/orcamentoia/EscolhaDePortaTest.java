package com.lucdev.orcamentoia;

import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

// A porta 8080 e das mais disputadas. Sem esta escolha, a aplicacao morre no
// boot com erro de socket — e no aplicativo de duplo clique isso aparece como
// uma janela que simplesmente nunca abre.
class EscolhaDePortaTest {

    @Test
    void usaAPortaPadraoQuandoEstaLivre() {
        int porta = OrcamentoIaApplication.primeiraPortaLivre(58080, 5);

        assertThat(porta).isEqualTo(58080);
    }

    // O caso que motivou o recurso: alguem ja esta na porta.
    @Test
    void pulaAPortaOcupada() throws Exception {
        try (ServerSocket ocupada = new ServerSocket(58090)) {
            assertThat(ocupada.isBound()).isTrue();

            int porta = OrcamentoIaApplication.primeiraPortaLivre(58090, 5);

            assertThat(porta).isEqualTo(58091);
        }
    }

    @Test
    void pulaVariasPortasSeguidasOcupadas() throws Exception {
        try (ServerSocket a = new ServerSocket(58100);
             ServerSocket b = new ServerSocket(58101);
             ServerSocket c = new ServerSocket(58102)) {
            assertThat(a.isBound() && b.isBound() && c.isBound()).isTrue();

            assertThat(OrcamentoIaApplication.primeiraPortaLivre(58100, 10)).isEqualTo(58103);
        }
    }

    // Quem definiu a porta manda: nesse caso nao ha busca, e o Spring falha com
    // a mensagem dele se estiver ocupada — a pessoa pediu aquela porta.
    @Test
    void escolhaExplicitaVenceABusca() {
        Properties p = new Properties();
        p.setProperty(OrcamentoIaApplication.PORTA, "9090");

        assertThat(OrcamentoIaApplication.escolherPorta(p)).isEqualTo(9090);
    }

    @Test
    void valorInvalidoCaiNaBuscaNormal() {
        Properties p = new Properties();
        p.setProperty(OrcamentoIaApplication.PORTA, "nao-e-numero");

        assertThat(OrcamentoIaApplication.escolherPorta(p))
                .isGreaterThanOrEqualTo(OrcamentoIaApplication.PORTA_PADRAO);
    }
}
