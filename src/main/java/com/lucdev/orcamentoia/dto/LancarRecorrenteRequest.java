package com.lucdev.orcamentoia.dto;

import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

// O valor real da conta deste mes. Vazio significa "usar a previsao".
public record LancarRecorrenteRequest(
        @Positive(message = "deve ser positivo") BigDecimal valor
) {
}
