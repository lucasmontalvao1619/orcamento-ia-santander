package com.lucdev.orcamentoia.dto;

// Uma opcao de categoria: "valor" e o que vai gravado na transacao, "rotulo" e
// o texto exibido na interface.
public record CategoriaResponse(
        String valor,
        String rotulo
) {
}
