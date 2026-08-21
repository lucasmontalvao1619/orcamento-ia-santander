package com.lucdev.orcamentoia.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

// Quando a aplicacao vira um executavel de duplo clique (jpackage), nao existe
// script de inicializacao para abrir a interface: o .exe ou o .app chamam a JVM
// direto. Sem isto, o usuario clicaria, o servidor subiria em silencio e nada
// apareceria na tela.
//
// Fica desligado por padrao de proposito: em container ou em servidor nao ha
// navegador para abrir, e tentar abrir um seria erro garantido. Quem liga e o
// proprio empacotamento, com -Dapp.abrir-navegador=true.
@Component
public class AberturaDoNavegador {

    private static final Logger log = LoggerFactory.getLogger(AberturaDoNavegador.class);

    private final boolean habilitado;
    private final String porta;

    public AberturaDoNavegador(
            @Value("${app.abrir-navegador:false}") boolean habilitado,
            @Value("${server.port:8080}") String porta) {
        this.habilitado = habilitado;
        this.porta = porta;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void abrir() {
        if (!habilitado) {
            return;
        }
        String url = "http://localhost:" + porta;
        List<String> comando = comandoDoSistema(url);
        if (comando.isEmpty()) {
            log.info("Sistema nao reconhecido para abrir o navegador. Acesse {}", url);
            return;
        }
        try {
            new ProcessBuilder(comando).start();
        } catch (IOException e) {
            // Falhar aqui nao pode derrubar a aplicacao: o servidor ja esta no
            // ar e continua utilizavel se o usuario abrir o endereco na mao.
            log.warn("Nao foi possivel abrir o navegador automaticamente. Acesse {}", url);
        }
    }

    // Nao usa java.awt.Desktop de proposito: o Spring Boot sobe com
    // java.awt.headless=true, e nesse modo o Desktop nao esta disponivel.
    // Chamar o comando do sistema funciona igual nos tres sistemas.
    // Visibilidade de pacote para o teste cobrir os tres sistemas sem precisar
    // rodar em cada um deles.
    List<String> comandoDoSistema(String url) {
        String so = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (so.contains("mac")) {
            return List.of("open", url);
        }
        if (so.contains("win")) {
            // O "" e o titulo da janela: sem ele, o start trata a URL como
            // titulo e nao abre nada.
            return List.of("cmd", "/c", "start", "", url);
        }
        if (so.contains("nux") || so.contains("nix")) {
            return List.of("xdg-open", url);
        }
        return List.of();
    }
}
