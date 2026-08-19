package com.lucdev.orcamentoia.dto;

import com.lucdev.orcamentoia.model.TipoTransacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record NovaTransacaoRequest(
        @NotBlank String descricao,
        @NotNull @Positive BigDecimal valor,
        @NotBlank String categoria,
        @NotNull TipoTransacao tipo
) {
}
