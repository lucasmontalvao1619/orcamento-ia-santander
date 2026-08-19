package com.lucdev.orcamentoia.service;

import com.lucdev.orcamentoia.model.TipoTransacao;
import com.lucdev.orcamentoia.repository.TransacaoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = "spring.ai.openai.api-key=test-key")
class TransacaoServiceTest {

    @Autowired
    private TransacaoService transacaoService;

    @Autowired
    private TransacaoRepository repository;

    @Test
    void deveCalcularSaldoComoReceitasMenosDespesas() {
        repository.deleteAll();
        transacaoService.registrar("Salario", new BigDecimal("3000.00"), "salario", TipoTransacao.RECEITA);
        transacaoService.registrar("Mercado", new BigDecimal("500.00"), "alimentacao", TipoTransacao.DESPESA);

        assertEquals(new BigDecimal("2500.00"), transacaoService.calcularSaldo());
    }

    @Test
    void deveRejeitarValorNaoPositivo() {
        assertThrows(IllegalArgumentException.class, () ->
                transacaoService.registrar("Invalida", new BigDecimal("-10.00"), "erro", TipoTransacao.DESPESA));
    }
}
