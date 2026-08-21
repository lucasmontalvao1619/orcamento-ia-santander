package com.lucdev.orcamentoia.controller;

import com.lucdev.orcamentoia.config.EncerramentoAutomatico;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SessaoController.class)
class SessaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EncerramentoAutomatico encerramento;

    // A interface so comeca a mandar sinais se a resposta for true; se este
    // campo vier errado, ou o app nunca fecha, ou o celular manda sinal a toa.
    @Test
    void informaQuandoEstaEmModoAplicativo() throws Exception {
        when(encerramento.isHabilitado()).thenReturn(true);

        mockMvc.perform(get("/api/sessao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modoAplicativo").value(true));
    }

    @Test
    void informaQuandoNaoEstaEmModoAplicativo() throws Exception {
        when(encerramento.isHabilitado()).thenReturn(false);

        mockMvc.perform(get("/api/sessao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modoAplicativo").value(false));
    }

    @Test
    void oSinalDeVidaChegaAoVigia() throws Exception {
        mockMvc.perform(post("/api/sessao/sinal"))
                .andExpect(status().isNoContent());

        verify(encerramento).registrarSinalDeVida();
    }
}
