package com.lucdev.orcamentoia.dto;

import jakarta.validation.constraints.NotBlank;

public record ChaveOpenAiRequest(
        @NotBlank(message = "e obrigatoria") String chave
) {
}
