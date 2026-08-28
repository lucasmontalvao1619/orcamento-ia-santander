package com.lucdev.orcamentoia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

// O agendamento existe para a verificacao de janela fechada do modo aplicativo.
@EnableScheduling
@SpringBootApplication
public class OrcamentoIaApplication {

    // Propriedade que a configuracao le para montar a URL do banco.
    static final String PASTA_DE_DADOS = "orcamento.dados";

    // Ligada pelo executavel empacotado (jpackage) via -Dapp.empacotado=true.
    static final String MARCA_DE_EMPACOTADO = "app.empacotado";

    static final String PORTA = "server.port";
    static final int PORTA_PADRAO = 8080;
    static final int PORTAS_TENTADAS = 20;

    public static void main(String[] args) {
        System.setProperty(PASTA_DE_DADOS, pastaDeDados(System.getProperties()));
        System.setProperty(PORTA, String.valueOf(escolherPorta(System.getProperties())));
        SpringApplication.run(OrcamentoIaApplication.class, args);
    }

    // A porta 8080 e das mais disputadas: outro projeto Spring, um Tomcat, um
    // servidor de desenvolvimento. Com ela ocupada, a aplicacao morria no boot
    // com um erro de socket — e no aplicativo de duplo clique o usuario nao
    // veria nem isso, so uma janela que nunca abre.
    //
    // Procurar a proxima porta livre custa alguns milissegundos e transforma um
    // erro fatal em nada. Quem definiu a porta explicitamente manda: nesse caso
    // nao ha busca, e o Spring falha com a mensagem dele, que e o certo — a
    // pessoa pediu aquela porta.
    static int escolherPorta(java.util.Properties propriedades) {
        String escolhida = propriedades.getProperty(PORTA);
        if (escolhida != null && !escolhida.isBlank()) {
            try {
                return Integer.parseInt(escolhida.trim());
            } catch (NumberFormatException ignorado) {
                // Valor invalido cai na busca normal.
            }
        }
        return primeiraPortaLivre(PORTA_PADRAO, PORTAS_TENTADAS);
    }

    static int primeiraPortaLivre(int inicial, int tentativas) {
        for (int porta = inicial; porta < inicial + tentativas; porta++) {
            if (estaLivre(porta)) {
                return porta;
            }
        }
        // Todas ocupadas: devolve a padrao para o Spring falhar com a mensagem
        // dele, em vez de a aplicacao subir num lugar imprevisivel.
        return inicial;
    }

    private static boolean estaLivre(int porta) {
        try (ServerSocket socket = new ServerSocket(porta)) {
            socket.setReuseAddress(true);
            return true;
        } catch (IOException ocupada) {
            return false;
        }
    }

    // Rodando pelo Maven ou por container, o banco fica em ./dados, ao lado do
    // projeto: e onde o usuario espera encontrar, e e para la que o volume do
    // Docker aponta.
    //
    // Ja o executavel de duplo clique e instalado numa pasta de programas, e o
    // diretorio de trabalho dele nao e o da aplicacao (no macOS e a raiz do
    // disco). Gravar "./dados" dali falharia por permissao, ou espalharia
    // bancos em lugares imprevisiveis. Por isso o app empacotado guarda os
    // lancamentos na pasta pessoal do usuario.
    static String pastaDeDados(Properties propriedades) {
        String escolhaExplicita = propriedades.getProperty(PASTA_DE_DADOS);
        if (escolhaExplicita != null && !escolhaExplicita.isBlank()) {
            return escolhaExplicita;
        }
        if (!Boolean.parseBoolean(propriedades.getProperty(MARCA_DE_EMPACOTADO))) {
            return "./dados";
        }
        Path pessoal = Paths.get(propriedades.getProperty("user.home", "."), ".orcamento-ia");
        return pessoal.toString();
    }
}
