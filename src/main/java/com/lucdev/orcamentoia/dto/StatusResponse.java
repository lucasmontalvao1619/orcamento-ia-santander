package com.lucdev.orcamentoia.dto;

public record StatusResponse(
        boolean iaConfigurada,
        boolean transcricaoServidor,
        String provedor,
        String mensagem
) {
}
