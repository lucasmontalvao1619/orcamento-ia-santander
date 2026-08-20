package com.lucdev.orcamentoia;

import com.lucdev.orcamentoia.service.AssistenteService;
import com.lucdev.orcamentoia.service.TranscricaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

// Unico teste que sobe o contexto inteiro: garante que a auto-configuracao do
// Spring AI continua ligando o ChatClient e a transcricao de audio. Nenhuma
// chamada de rede acontece aqui, so a criacao dos beans.
@SpringBootTest(properties = "spring.ai.openai.api-key=chave-de-teste")
class OrcamentoIaApplicationTests {

    @Autowired
    private AssistenteService assistenteService;

    @Autowired
    private TranscricaoService transcricaoService;

    @Test
    void oContextoSobeComOsServicosDeIaConfigurados() {
        assertThat(assistenteService).isNotNull();
        assertThat(transcricaoService).isNotNull();
    }
}
