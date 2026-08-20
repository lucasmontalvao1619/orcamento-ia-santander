package com.lucdev.orcamentoia.dto;

import com.lucdev.orcamentoia.model.TipoTransacao;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

// Todos os campos sao opcionais: envie apenas o que quer mudar.
public record AtualizarTransacaoRequest(
        String descricao,
        @Positive(message = "deve ser positivo") BigDecimal valor,
        String categoria,
        TipoTransacao tipo
) {
}
