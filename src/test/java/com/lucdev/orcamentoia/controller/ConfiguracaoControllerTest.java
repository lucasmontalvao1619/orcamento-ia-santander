package com.lucdev.orcamentoia.controller;

import com.lucdev.orcamentoia.model.Configuracao;
import com.lucdev.orcamentoia.service.ConfiguracaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConfiguracaoController.class)
class ConfiguracaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConfiguracaoService configuracaoService;

    // configurado=false e o gatilho da tela de boas-vindas: se este campo vier
    // errado, ou o onboarding nao aparece, ou reaparece para quem ja configurou.
    @Test
    void indicaQuandoOSalarioAindaNaoFoiConfigurado() throws Exception {
        when(configuracaoService.obter()).thenReturn(new Configuracao());

        mockMvc.perform(get("/api/configuracao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configurado").value(false));
    }

    @Test
    void devolveOSalarioJaConfigurado() throws Exception {
        when(configuracaoService.obter()).thenReturn(configuracao("3000.00", 15));

        mockMvc.perform(get("/api/configuracao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configurado").value(true))
                .andExpect(jsonPath("$.salario").value(3000.00))
                .andExpect(jsonPath("$.diaRecebimento").value(15));
    }

    @Test
    void defineOSalario() throws Exception {
        when(configuracaoService.definirSalario(new BigDecimal("3000.00"), 15))
                .thenReturn(configuracao("3000.00", 15));

        mockMvc.perform(put("/api/configuracao/salario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"salario":3000.00,"diaRecebimento":15}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salario").value(3000.00));
    }

    @Test
    void recusaSalarioNegativoComProblemDetail() throws Exception {
        mockMvc.perform(put("/api/configuracao/salario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"salario":-100.00,"diaRecebimento":15}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsString("deve ser positivo")));

        verify(configuracaoService, never()).definirSalario(any(), any());
    }

    // Dia 32 nao existe. A regra vale no @Valid do controller e tambem no
    // service, para a entrada da IA cair na mesma checagem.
    @Test
    void recusaDiaDeRecebimentoForaDoMes() throws Exception {
        mockMvc.perform(put("/api/configuracao/salario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"salario":3000.00,"diaRecebimento":32}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsString("entre 1 e 31")));

        verify(configuracaoService, never()).definirSalario(any(), any());
    }

    // A interface monta os selects a partir daqui em vez de repetir as listas no
    // JavaScript: as categorias tem uma fonte unica, que e o enum do backend.
    @Test
    void listaAsCategoriasDeDespesaEDeReceita() throws Exception {
        mockMvc.perform(get("/api/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.despesas.length()").value(5))
                .andExpect(jsonPath("$.receitas.length()").value(3))
                .andExpect(jsonPath("$.despesas[*].valor").value(
                        org.hamcrest.Matchers.hasItem("alimentacao")))
                .andExpect(jsonPath("$.receitas[*].valor").value(
                        org.hamcrest.Matchers.hasItem("salario")));
    }

    private Configuracao configuracao(String salario, Integer diaRecebimento) {
        Configuracao configuracao = new Configuracao();
        configuracao.setSalario(new BigDecimal(salario));
        configuracao.setDiaRecebimento(diaRecebimento);
        return configuracao;
    }
}
