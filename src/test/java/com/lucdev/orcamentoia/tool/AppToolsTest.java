package com.lucdev.orcamentoia.tool;

import com.lucdev.orcamentoia.config.Autoria;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Estas duas ferramentas existem para o modelo nao responder de cabeca sobre o
// proprio app. So que o texto delas e escrito a mao: quando um recurso muda, e
// aqui que a mentira aparece. O teste existe para prender esse texto ao codigo.
class AppToolsTest {

    private final AppTools appTools = new AppTools();

    @Test
    void recursosCitamOQueOAppRealmenteFaz() {
        String resposta = appTools.consultarRecursosDoApp();

        assertThat(resposta).contains("Corrigir", "apagar", "Porquinho", "CDI", "Categorias");
    }

    // O banco deixou de ser em memoria quando a persistencia passou a ser em
    // arquivo, e por um tempo esta ferramenta continuou dizendo o contrario.
    @Test
    void recursosNaoPrometemQueOsDadosSeraoPerdidos() {
        String resposta = appTools.consultarRecursosDoApp();

        assertThat(resposta)
                .doesNotContain("em memoria")
                .doesNotContain("apagados quando");
    }

    @Test
    void autorSaiDaFonteUnicaDeAutoria() {
        String resposta = appTools.consultarAutor();

        assertThat(resposta).contains(Autoria.AUTOR, Autoria.GITHUB, Autoria.SITE, Autoria.PROJETO);
    }
}
