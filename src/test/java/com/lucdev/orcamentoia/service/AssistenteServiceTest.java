package com.lucdev.orcamentoia.service;

import com.lucdev.orcamentoia.tool.AppTools;
import com.lucdev.orcamentoia.tool.FinancasTools;
import com.lucdev.orcamentoia.tool.InvestimentoTools;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssistenteServiceTest {

    @Mock
    private FinancasTools financasTools;

    @Mock
    private InvestimentoTools investimentoTools;

    @Mock
    private AppTools appTools;

    private AssistenteService comRespostaDoModelo(String resposta) {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClient.prompt().user(anyString()).tools(any(Object[].class)).call().content())
                .thenReturn(resposta);
        return new AssistenteService(chatClient, financasTools, investimentoTools, appTools);
    }

    @Test
    void devolveARespostaDoModeloQuandoEleResponderEmLinguagemNatural() {
        AssistenteService service = comRespostaDoModelo("O saldo atual do orcamento e de R$ 2925,00.");

        assertThat(service.processarComando("qual o saldo?"))
                .isEqualTo("O saldo atual do orcamento e de R$ 2925,00.");
    }

    // Modelos locais menores as vezes escrevem o tool call como texto; esse JSON
    // nao pode chegar na tela do usuario.
    @Test
    void substituiOVazamentoDeToolCallPorUmaRespostaLegivel() {
        AssistenteService service = comRespostaDoModelo(
                "{\"name\": \"consultarTransacoes\", \"parameters\": {\"id\": \"2\"}}");

        assertThat(service.processarComando("liste minhas transacoes"))
                .doesNotContain("parameters")
                .doesNotContain("{")
                .contains("Confira o saldo");
    }

    @Test
    void trocaRespostaVaziaPelaMensagemPadrao() {
        assertThat(comRespostaDoModelo("   ").processarComando("oi")).contains("Confira o saldo");
        assertThat(comRespostaDoModelo(null).processarComando("oi")).contains("Confira o saldo");
    }
}
