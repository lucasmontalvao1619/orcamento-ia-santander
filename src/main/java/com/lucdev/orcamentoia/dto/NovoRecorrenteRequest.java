package com.lucdev.orcamentoia.dto;

import com.lucdev.orcamentoia.model.TipoTransacao;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record NovoRecorrenteRequest(
        @NotBlank(message = "e obrigatoria") String descricao,
        String categoria,
        @NotNull(message = "e obrigatorio: RECEITA ou DESPESA") TipoTransacao tipo,
        // Sem @NotNull: cartao de credito nao tem valor conhecido de antemao.
        @Positive(message = "deve ser positivo") BigDecimal valorPrevisto,
        @Min(value = 1, message = "deve estar entre 1 e 31")
        @Max(value = 31, message = "deve estar entre 1 e 31") Integer diaVencimento
) {
}
