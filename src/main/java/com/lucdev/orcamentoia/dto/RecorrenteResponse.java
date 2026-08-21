package com.lucdev.orcamentoia.dto;

import com.lucdev.orcamentoia.model.Recorrente;
import com.lucdev.orcamentoia.model.TipoTransacao;

import java.math.BigDecimal;

public record RecorrenteResponse(
        Long id,
        String descricao,
        String categoria,
        TipoTransacao tipo,
        BigDecimal valorPrevisto,
        Integer diaVencimento
) {
    public static RecorrenteResponse de(Recorrente r) {
        return new RecorrenteResponse(r.getId(), r.getDescricao(), r.getCategoria(),
                r.getTipo(), r.getValorPrevisto(), r.getDiaVencimento());
    }
}
