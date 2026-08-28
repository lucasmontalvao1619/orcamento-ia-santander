package com.lucdev.orcamentoia.controller;

import com.lucdev.orcamentoia.model.TipoTransacao;
import com.lucdev.orcamentoia.model.Transacao;
import com.lucdev.orcamentoia.service.TransacaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransacaoController.class)
class TransacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransacaoService transacaoService;

    @Test
    void criaUmaTransacaoValida() throws Exception {
        Transacao transacao = new Transacao("Almoco", new BigDecimal("50.00"), "alimentacao", TipoTransacao.DESPESA);
        transacao.setId(1L);
        when(transacaoService.criar(any())).thenReturn(transacao);

        mockMvc.perform(post("/api/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":"Almoco","valor":50.00,"categoria":"alimentacao","tipo":"DESPESA"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.descricao").value("Almoco"));
    }

    // Valor negativo e barrado pelo @Valid antes de chegar ao service, e a
    // resposta sai como ProblemDetail, igual as falhas de regra de negocio.
    @Test
    void recusaValorNegativoComProblemDetail() throws Exception {
        mockMvc.perform(post("/api/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":"Invalida","valor":-10.00,"categoria":"erro","tipo":"DESPESA"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("deve ser positivo")));

        verify(transacaoService, never()).criar(any());
    }

    @Test
    void devolveOSaldoAtual() throws Exception {
        when(transacaoService.calcularSaldo()).thenReturn(new BigDecimal("2490.50"));

        mockMvc.perform(get("/api/transacoes/saldo"))
                .andExpect(status().isOk())
                .andExpect(content().string("2490.50"));
    }

    @Test
    void filtraAListagemPorTipo() throws Exception {
        when(transacaoService.listarPorTipo(TipoTransacao.RECEITA)).thenReturn(List.of(
                new Transacao("Salario", new BigDecimal("3000.00"), "salario", TipoTransacao.RECEITA)));

        mockMvc.perform(get("/api/transacoes").param("tipo", "RECEITA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].descricao").value("Salario"));

        verify(transacaoService, never()).listarTodas();
    }
}
