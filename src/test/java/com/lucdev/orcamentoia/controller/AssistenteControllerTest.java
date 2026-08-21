package com.lucdev.orcamentoia.controller;

import com.lucdev.orcamentoia.service.AssistenteService;
import com.lucdev.orcamentoia.service.TranscricaoService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssistenteController.class)
class AssistenteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssistenteService assistenteService;

    @MockitoBean
    private TranscricaoService transcricaoService;

    @Test
    void processaUmComandoDeTexto() throws Exception {
        when(assistenteService.processarComando("gastei 60 no uber"))
                .thenReturn("Transacao registrada com sucesso.");

        mockMvc.perform(post("/api/assistente/texto").param("comando", "gastei 60 no uber"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.textoTranscrito").value("gastei 60 no uber"))
                .andExpect(jsonPath("$.respostaAssistente").value("Transacao registrada com sucesso."));
    }

    // O audio passa pela transcricao antes do assistente, e o texto reconhecido
    // volta junto na resposta: e por ele que o usuario confere se foi entendido.
    @Test
    void transcreveOAudioAntesDeProcessarOComando() throws Exception {
        when(transcricaoService.transcrever(any())).thenReturn("quanto eu tenho guardado");
        when(assistenteService.processarComando("quanto eu tenho guardado"))
                .thenReturn("No porquinho existem R$ 800,00.");

        mockMvc.perform(multipart("/api/assistente/audio")
                        .file(new MockMultipartFile("arquivo", "comando.webm",
                                "audio/webm", "audio-simulado".getBytes())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.textoTranscrito").value("quanto eu tenho guardado"))
                .andExpect(jsonPath("$.respostaAssistente").value("No porquinho existem R$ 800,00."));
    }

    // Falha do provedor nao pode virar 500: a interface distingue "recusou" de
    // "esta fora do ar" para dizer ao usuario o que fazer em cada caso.
    @Test
    void provedorQueRecusaViraBadGateway() throws Exception {
        when(assistenteService.processarComando(any()))
                .thenThrow(new NonTransientAiException("401 Unauthorized"));

        mockMvc.perform(post("/api/assistente/texto").param("comando", "ola"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.title").value("Falha no provedor de IA"));
    }

    @Test
    void provedorIndisponivelViraServiceUnavailable() throws Exception {
        when(assistenteService.processarComando(any()))
                .thenThrow(new TransientAiException("429 Too Many Requests"));

        mockMvc.perform(post("/api/assistente/texto").param("comando", "ola"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Provedor de IA indisponivel"));
    }

    @Test
    void ollamaForaDoArViraServiceUnavailable() throws Exception {
        when(assistenteService.processarComando(any()))
                .thenThrow(new ResourceAccessException("Connection refused"));

        mockMvc.perform(post("/api/assistente/texto").param("comando", "ola"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Modelo local indisponivel"))
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsString("ollama serve")));
    }

    // O Ollama roda local e nao usa chave, entao o assistente esta utilizavel
    // mesmo sem OPENAI_API_KEY nenhuma no ambiente.
    @Test
    void statusComOllamaNaoExigeChave() throws Exception {
        mockMvc.perform(get("/api/assistente/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provedor").value("ollama"))
                .andExpect(jsonPath("$.iaConfigurada").value(true));

        verify(assistenteService, never()).processarComando(any());
    }

    @Nested
    @TestPropertySource(properties = {
            "spring.ai.model.chat=openai",
            "spring.ai.openai.api-key=chave-nao-configurada"
    })
    class QuandoOProvedorEOpenAiSemChave {

        @Autowired
        private MockMvc mockMvc;

        // O valor default de application.properties conta como chave ausente:
        // sem isto a interface diria que o assistente esta pronto e o usuario
        // so descobriria o contrario ao mandar o primeiro comando.
        @Test
        void statusAvisaQueFaltaAChave() throws Exception {
            mockMvc.perform(get("/api/assistente/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.provedor").value("openai"))
                    .andExpect(jsonPath("$.iaConfigurada").value(false))
                    .andExpect(jsonPath("$.mensagem").value(
                            org.hamcrest.Matchers.containsString("OPENAI_API_KEY")));
        }
    }
}
