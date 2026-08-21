package com.lucdev.orcamentoia.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// O executavel de duplo clique depende deste mapeamento: se o comando do
// sistema estiver errado, o usuario clica, o servidor sobe e a tela nunca
// aparece. Como o CI empacota para Windows e macOS, os tres casos sao testados
// aqui em vez de so no sistema onde a suite roda.
class AberturaDoNavegadorTest {

    private static final String SO = "os.name";
    private final String originalSo = System.getProperty(SO);

    @AfterEach
    void restaurarSistema() {
        if (originalSo == null) {
            System.clearProperty(SO);
        } else {
            System.setProperty(SO, originalSo);
        }
    }

    @Test
    void noMacUsaOComandoOpen() {
        System.setProperty(SO, "Mac OS X");

        assertThat(comandos()).containsExactly("open", "http://localhost:8080");
    }

    // O "" e o titulo da janela. Sem ele o start trata a URL como titulo e nao
    // abre navegador nenhum — falha classica e silenciosa no Windows.
    @Test
    void noWindowsUsaStartComTituloVazio() {
        System.setProperty(SO, "Windows 11");

        assertThat(comandos()).containsExactly("cmd", "/c", "start", "", "http://localhost:8080");
    }

    @Test
    void noLinuxUsaXdgOpen() {
        System.setProperty(SO, "Linux");

        assertThat(comandos()).containsExactly("xdg-open", "http://localhost:8080");
    }

    @Test
    void sistemaDesconhecidoNaoInventaComando() {
        System.setProperty(SO, "SistemaExotico");

        assertThat(comandos()).isEmpty();
    }

    @Test
    void respeitaAPortaConfigurada() {
        System.setProperty(SO, "Mac OS X");

        List<String> comando = new AberturaDoNavegador(true, "9090")
                .comandoDoSistema("http://localhost:9090");

        assertThat(comando).containsExactly("open", "http://localhost:9090");
    }

    // Em container ou servidor nao existe navegador para abrir; por isso o
    // recurso nasce desligado e so o empacotamento o liga.
    @Test
    void desligadoPorPadraoNaoTentaAbrirNada() {
        System.setProperty(SO, "Mac OS X");

        AberturaDoNavegador desligado = new AberturaDoNavegador(false, "8080");

        // Nao deve lancar nem disparar processo: o metodo sai antes de montar
        // o comando quando o recurso esta desligado.
        desligado.abrir();
    }

    private List<String> comandos() {
        return new AberturaDoNavegador(true, "8080").comandoDoSistema("http://localhost:8080");
    }
}
