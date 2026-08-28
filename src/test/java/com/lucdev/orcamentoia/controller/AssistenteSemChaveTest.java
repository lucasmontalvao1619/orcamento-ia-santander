package com.lucdev.orcamentoia.controller;

import com.lucdev.orcamentoia.model.Configuracao;
import com.lucdev.orcamentoia.service.AssistenteService;
import com.lucdev.orcamentoia.service.ConfiguracaoService;
import com.lucdev.orcamentoia.service.TranscricaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Classe separada, e nao aninhada, porque este cenario precisa de um contexto
// proprio: a chave de ambiente e zerada aqui. Aninhar criava dois overrides do
// mesmo bean e o contexto nem carregava.
@WebMvcTest(AssistenteController.class)
@TestPropertySource(properties = {
        "spring.ai.model.chat=openai",
        "spring.ai.openai.api-key=chave-nao-configurada"
})
class AssistenteSemChaveTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssistenteService assistenteService;

    @MockitoBean
    private TranscricaoService transcricaoService;

    @MockitoBean
    private ConfiguracaoService configuracaoService;

    // Sem chave, o assistente NAO esta indisponivel: o interpretador proprio
    // atende os comandos escritos. A mensagem precisa dizer o que realmente
    // falta — voz e frases livres — em vez de bloquear o que funciona.
    @Test
    void semChaveOAssistenteSegueUtilEAMensagemDizOQueFalta() throws Exception {
        when(configuracaoService.obter()).thenReturn(new Configuracao());

        mockMvc.perform(get("/api/assistente/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provedor").value("openai"))
                .andExpect(jsonPath("$.iaConfigurada").value(false))
                .andExpect(jsonPath("$.mensagem").value(
                        org.hamcrest.Matchers.containsString("modo local")))
                .andExpect(jsonPath("$.mensagem").value(
                        org.hamcrest.Matchers.containsString("voz")));
    }
}
