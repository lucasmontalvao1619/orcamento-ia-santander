package com.lucdev.orcamentoia.dto;

import com.lucdev.orcamentoia.model.Configuracao;

import java.math.BigDecimal;

public record ConfiguracaoResponse(
        boolean configurado,
        BigDecimal salario
) {
    public static ConfiguracaoResponse de(Configuracao configuracao) {
        return new ConfiguracaoResponse(configuracao.isConfigurado(), configuracao.getSalario());
    }
}
