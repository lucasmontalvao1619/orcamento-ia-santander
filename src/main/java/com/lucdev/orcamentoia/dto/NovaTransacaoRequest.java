package com.lucdev.orcamentoia.dto;

import com.lucdev.orcamentoia.model.TipoTransacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record NovaTransacaoRequest(
        @NotBlank(message = "e obrigatoria") String descricao,
        @NotNull(message = "e obrigatorio") @Positive(message = "deve ser positivo") BigDecimal valor,
        @NotBlank(message = "e obrigatoria") String categoria,
        @NotNull(message = "e obrigatorio: RECEITA ou DESPESA") TipoTransacao tipo
) {
}
