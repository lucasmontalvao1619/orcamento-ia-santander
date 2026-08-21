package com.lucdev.orcamentoia.controller;

import com.lucdev.orcamentoia.exception.RecursoNaoEncontradoException;
import com.lucdev.orcamentoia.model.Investimento;
import com.lucdev.orcamentoia.model.TipoInvestimento;
import com.lucdev.orcamentoia.service.InvestimentoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvestimentoController.class)
class InvestimentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InvestimentoService investimentoService;

    @Test
    void registraUmAporte() throws Exception {
        Investimento aporte = new Investimento("Viagem", new BigDecimal("800.00"), TipoInvestimento.APORTE);
        aporte.setId(1L);
        when(investimentoService.registrar("Viagem", new BigDecimal("800.00"), TipoInvestimento.APORTE))
                .thenReturn(aporte);

        mockMvc.perform(post("/api/investimentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":"Viagem","valor":800.00,"tipo":"APORTE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.tipo").value("APORTE"));
    }

    @Test
    void recusaValorNegativoComProblemDetail() throws Exception {
        mockMvc.perform(post("/api/investimentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":"Invalido","valor":-50.00,"tipo":"APORTE"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsString("deve ser positivo")));

        verify(investimentoService, never()).registrar(any(), any(), any());
    }

    @Test
    void listaOsMovimentos() throws Exception {
        when(investimentoService.listarTodos()).thenReturn(List.of(
                new Investimento("Reserva", new BigDecimal("500.00"), TipoInvestimento.APORTE)));

        mockMvc.perform(get("/api/investimentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].descricao").value("Reserva"));
    }

    // O rendimento e calculado com muitas casas decimais; a API arredonda para
    // centavos, senao a interface exibiria um numero com dez casas.
    @Test
    void oResumoSaiArredondadoEmCentavos() throws Exception {
        when(investimentoService.calcularTotal()).thenReturn(new BigDecimal("1000.00"));
        when(investimentoService.somar(TipoInvestimento.APORTE)).thenReturn(new BigDecimal("1200.00"));
        when(investimentoService.somar(TipoInvestimento.RETIRADA)).thenReturn(new BigDecimal("200.00"));
        when(investimentoService.calcularRendimento()).thenReturn(new BigDecimal("34.56789"));
        when(investimentoService.calcularSaldoComRendimento()).thenReturn(new BigDecimal("1034.56789"));
        when(investimentoService.getCdiAnual()).thenReturn(new BigDecimal("0.1065"));

        mockMvc.perform(get("/api/investimentos/resumo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rendimento").value(34.57))
                .andExpect(jsonPath("$.totalComRendimento").value(1034.57))
                .andExpect(jsonPath("$.cdiAnual").value(0.1065));
    }

    @Test
    void apagarUmMovimentoDevolveNoContent() throws Exception {
        when(investimentoService.apagar(1L)).thenReturn(
                new Investimento("Engano", new BigDecimal("50.00"), TipoInvestimento.APORTE));

        mockMvc.perform(delete("/api/investimentos/1"))
                .andExpect(status().isNoContent());

        verify(investimentoService).apagar(1L);
    }

    @Test
    void apagarIdInexistenteDevolveNotFound() throws Exception {
        doThrow(new RecursoNaoEncontradoException("Nao existe movimento com o id 99."))
                .when(investimentoService).apagar(99L);

        mockMvc.perform(delete("/api/investimentos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
