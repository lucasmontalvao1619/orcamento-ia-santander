package com.lucdev.orcamentoia.dto;

import com.lucdev.orcamentoia.model.TipoTransacao;
import com.lucdev.orcamentoia.model.Transacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransacaoResponse(
        Long id,
        String descricao,
        BigDecimal valor,
        String categoria,
        TipoTransacao tipo,
        LocalDateTime dataHora
) {
    public static TransacaoResponse de(Transacao transacao) {
        return new TransacaoResponse(
                transacao.getId(),
                transacao.getDescricao(),
                transacao.getValor(),
                transacao.getCategoria(),
                transacao.getTipo(),
                transacao.getDataHora()
        );
    }
}
