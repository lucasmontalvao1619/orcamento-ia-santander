package com.lucdev.orcamentoia;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

// Onde o banco e gravado decide se o executavel de duplo clique sobe ou morre
// no boot: instalado numa pasta de programas, ele nao tem permissao para criar
// "./dados" no diretorio de trabalho — que no macOS e a raiz do disco.
class PastaDeDadosTest {

    @Test
    void noUsoNormalGravaAoLadoDoProjeto() {
        assertThat(OrcamentoIaApplication.pastaDeDados(new Properties())).isEqualTo("./dados");
    }

    // A comparacao e de caminho, nao de texto: o Windows monta com barra
    // invertida, e uma string fixa aqui passaria no Mac e quebraria o build do
    // executavel para Windows — foi o que aconteceu.
    @Test
    void empacotadoGravaNaPastaPessoalDoUsuario() {
        Properties p = new Properties();
        p.setProperty(OrcamentoIaApplication.MARCA_DE_EMPACOTADO, "true");
        p.setProperty("user.home", "/Users/fulano");

        assertThat(Paths.get(OrcamentoIaApplication.pastaDeDados(p)))
                .isEqualTo(Paths.get("/Users/fulano", ".orcamento-ia"));
    }

    // Quem define a pasta na mao manda em qualquer heuristica: e assim que o
    // Docker aponta o volume e que da para separar ambientes.
    @Test
    void escolhaExplicitaVenceTudo() {
        Properties p = new Properties();
        p.setProperty(OrcamentoIaApplication.MARCA_DE_EMPACOTADO, "true");
        p.setProperty("user.home", "/Users/fulano");
        p.setProperty(OrcamentoIaApplication.PASTA_DE_DADOS, "/dados/producao");

        assertThat(OrcamentoIaApplication.pastaDeDados(p)).isEqualTo("/dados/producao");
    }

    @Test
    void valorEmBrancoNaoContaComoEscolha() {
        Properties p = new Properties();
        p.setProperty(OrcamentoIaApplication.PASTA_DE_DADOS, "   ");

        assertThat(OrcamentoIaApplication.pastaDeDados(p)).isEqualTo("./dados");
    }
}
