package com.lucdev.orcamentoia.dto;

import com.lucdev.orcamentoia.model.TipoTransacao;

import java.math.BigDecimal;

// Uma linha da tela de fechar o mes.
public record ItemDoMesResponse(
        Long id,
        String descricao,
        String categoria,
        TipoTransacao tipo,
        BigDecimal valorPrevisto,
        Integer diaVencimento,
        // Ja lancado neste mes: a tela mostra como concluido e nao pede valor,
        // e o fechamento pula, para nao dobrar a conta.
        boolean jaLancado,
        BigDecimal valorLancado
) {
}
