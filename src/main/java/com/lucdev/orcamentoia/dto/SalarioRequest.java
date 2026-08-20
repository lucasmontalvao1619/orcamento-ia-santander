package com.lucdev.orcamentoia.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record SalarioRequest(
        @NotNull(message = "e obrigatorio") @Positive(message = "deve ser positivo") BigDecimal salario
) {
}
