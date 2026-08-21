package com.lucdev.orcamentoia.dto;

// modoAplicativo=true significa que a aplicacao esta rodando como programa de
// duplo clique, e que fechar a janela deve encerrar o processo.
public record SessaoResponse(
        boolean modoAplicativo,
        // Em container o modelo roda so em CPU e as respostas demoram bem mais.
        // A interface avisa para ninguem confundir lentidao com travamento.
        boolean emContainer
) {
}
