package com.lucdev.orcamentoia.dto;

import com.lucdev.orcamentoia.model.TipoTransacao;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

// Campos opcionais: o que vier nulo fica como esta. Para tirar a previsao e
// deixar a conta como de valor variavel, envie limparPrevisao=true — nulo
// sozinho significaria "nao mexer" e nao teria como expressar isso.
public record EditarRecorrenteRequest(
        String descricao,
        String categoria,
        TipoTransacao tipo,
        @Positive(message = "deve ser positivo") BigDecimal valorPrevisto,
        @Min(value = 1, message = "deve estar entre 1 e 31")
        @Max(value = 31, message = "deve estar entre 1 e 31") Integer diaVencimento,
        boolean limparPrevisao
) {
}
