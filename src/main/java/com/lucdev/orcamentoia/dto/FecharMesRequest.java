package com.lucdev.orcamentoia.dto;

import java.math.BigDecimal;
import java.util.Map;

// Id do item fixo -> valor real deste mes. Valor nulo usa a previsao.
public record FecharMesRequest(
        Map<Long, BigDecimal> valores
) {
}
