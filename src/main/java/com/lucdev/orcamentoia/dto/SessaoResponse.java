package com.lucdev.orcamentoia.dto;

// modoAplicativo=true significa que a aplicacao esta rodando como programa de
// duplo clique, e que fechar a janela deve encerrar o processo.
public record SessaoResponse(
        boolean modoAplicativo
) {
}
