package com.lucdev.orcamentoia.controller;

import com.lucdev.orcamentoia.config.Autoria;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Este endpoint e a razao de a autoria morar no backend: a interface pergunta
// quem fez o projeto em vez de trazer o nome escrito no HTML.
@WebMvcTest(SobreController.class)
class SobreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void devolveAAutoriaVindaDaFonteUnica() throws Exception {
        mockMvc.perform(get("/api/sobre"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projeto").value(Autoria.PROJETO))
                .andExpect(jsonPath("$.autor").value(Autoria.AUTOR))
                .andExpect(jsonPath("$.github").value(Autoria.GITHUB))
                .andExpect(jsonPath("$.site").value(Autoria.SITE));
    }

    // Os scripts de inicializacao usam este endpoint como sinal de "a aplicacao
    // subiu": ele nao depende do banco nem do provedor de IA.
    @Test
    void respondeSemDependerDeNenhumServico() throws Exception {
        mockMvc.perform(get("/api/sobre"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value(Autoria.DESCRICAO));
    }
}
