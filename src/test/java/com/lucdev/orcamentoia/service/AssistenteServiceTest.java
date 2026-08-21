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
import com.lucdev.orcamentoia.config.ClienteDeChat;
import com.lucdev.orcamentoia.model.Configuracao;
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
        // O cliente e resolvido na hora do uso, porque a chave pode ter sido
        // informada na interface depois que a aplicacao subiu.
        ClienteDeChat clienteDeChat = mock(ClienteDeChat.class);
        when(clienteDeChat.obter()).thenReturn(chatClient);

        // Estes testes exercitam o caminho da IA, entao a configuracao precisa
        // ter chave: sem ela o assistente usaria o interpretador proprio.
        Configuracao comChave = new Configuracao();
        comChave.setChaveOpenAi("sk-teste");
        ConfiguracaoService configuracaoService = mock(ConfiguracaoService.class);
        when(configuracaoService.obter()).thenReturn(comChave);
        InterpretadorDeComandos interpretador = mock(InterpretadorDeComandos.class);
        // A cadeia espelha a do service: prompt -> user -> advisors -> tools -> call.
        when(chatClient.prompt().user(anyString()).advisors(any(java.util.function.Consumer.class))
                .tools(any(Object[].class)).call().content())
                .thenReturn(resposta);
        return new AssistenteService(interpretador, configuracaoService, clienteDeChat,
                financasTools, investimentoTools, appTools);
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
