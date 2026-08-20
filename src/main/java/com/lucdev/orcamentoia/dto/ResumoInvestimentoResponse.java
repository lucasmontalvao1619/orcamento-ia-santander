package com.lucdev.orcamentoia.dto;

import java.math.BigDecimal;

public record ResumoInvestimentoResponse(
        BigDecimal total,
        BigDecimal aportes,
        BigDecimal retiradas
) {
}
