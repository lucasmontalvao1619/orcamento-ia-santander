package com.lucdev.orcamentoia.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record SalarioRequest(
        @NotNull(message = "e obrigatorio") @Positive(message = "deve ser positivo") BigDecimal salario,
        @Min(value = 1, message = "deve estar entre 1 e 31") @Max(value = 31, message = "deve estar entre 1 e 31")
        Integer diaRecebimento
) {
}
