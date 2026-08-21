package com.lucdev.orcamentoia.config;

import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

// Descobre se a aplicacao esta rodando dentro de um container.
//
// Serve para a interface avisar sobre a lentidao: em container o modelo roda so
// em CPU, e no macOS isso custa cerca de seis vezes o tempo de uma resposta
// (medido: 1min50s contra 18s). Sem o aviso, quem clicar e esperar dois minutos
// vai concluir que o aplicativo travou.
@Component
public class AmbienteDeExecucao {

    private final boolean emContainer;

    public AmbienteDeExecucao() {
        this(Files.exists(Path.of("/.dockerenv")) || System.getenv("OLLAMA_URL") != null);
    }

    // Visibilidade de pacote para o teste cobrir os dois casos sem depender de
    // onde a suite roda.
    AmbienteDeExecucao(boolean emContainer) {
        this.emContainer = emContainer;
    }

    public boolean isEmContainer() {
        return emContainer;
    }
}
