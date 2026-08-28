package com.lucdev.orcamentoia.dto;

public record SobreResponse(
        String projeto,
        String descricao,
        String autor,
        String github,
        String linkedin,
        String instagram,
        String site
) {
}
