package com.lucdev.orcamentoia.dto;

import java.util.List;

public record CategoriasResponse(
        List<CategoriaResponse> despesas,
        List<CategoriaResponse> receitas
) {
}
