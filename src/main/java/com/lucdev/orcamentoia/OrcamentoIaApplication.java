package com.lucdev.orcamentoia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

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

    public static void main(String[] args) {
        System.setProperty(PASTA_DE_DADOS, pastaDeDados(System.getProperties()));
        SpringApplication.run(OrcamentoIaApplication.class, args);
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
