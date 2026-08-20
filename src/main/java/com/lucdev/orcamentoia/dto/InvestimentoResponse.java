package com.lucdev.orcamentoia.dto;

import com.lucdev.orcamentoia.model.Investimento;
import com.lucdev.orcamentoia.model.TipoInvestimento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InvestimentoResponse(
        Long id,
        String descricao,
        BigDecimal valor,
        TipoInvestimento tipo,
        LocalDateTime dataHora
) {
    public static InvestimentoResponse de(Investimento investimento) {
        return new InvestimentoResponse(
                investimento.getId(),
                investimento.getDescricao(),
                investimento.getValor(),
                investimento.getTipo(),
                investimento.getDataHora());
    }
}
