package com.lucdev.orcamentoia.dto;

import com.lucdev.orcamentoia.model.TipoInvestimento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record NovoInvestimentoRequest(
        @NotBlank(message = "e obrigatoria") String descricao,
        @NotNull(message = "e obrigatorio") @Positive(message = "deve ser positivo") BigDecimal valor,
        @NotNull(message = "e obrigatorio: APORTE ou RETIRADA") TipoInvestimento tipo
) {
}
