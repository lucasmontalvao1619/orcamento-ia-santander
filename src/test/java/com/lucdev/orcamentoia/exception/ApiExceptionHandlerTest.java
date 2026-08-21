package com.lucdev.orcamentoia.exception;

import org.junit.jupiter.api.Test;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;

// O texto que sai daqui e o que o usuario le quando algo falha. Duas causas
// diferentes pedem acoes opostas — conta sem credito e problema de cobranca,
// chave invalida e de configuracao — e confundir as duas manda a pessoa
// procurar no lugar errado. Aconteceu em uso real.
class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void contaSemCreditoApontaACobranca() {
        ProblemDetail p = handler.provedorDeIaRecusou(
                new NonTransientAiException("429 - {\"code\": \"insufficient_quota\"}"));

        assertThat(p.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
        assertThat(p.getDetail()).contains("sem credito").contains("billing");
        // Nao pode sugerir conferir a chave: a chave esta certa.
        assertThat(p.getDetail()).doesNotContain("copiou");
    }

    @Test
    void chaveInvalidaApontaAConfiguracao() {
        ProblemDetail p = handler.provedorDeIaRecusou(
                new NonTransientAiException("401 - {\"code\": \"invalid_api_key\"}"));

        assertThat(p.getDetail()).contains("recusada").contains("Configuracoes");
        assertThat(p.getDetail()).doesNotContain("credito");
    }

    @Test
    void causaDesconhecidaCaiNaMensagemGenerica() {
        ProblemDetail p = handler.provedorDeIaRecusou(new NonTransientAiException("400 - algo novo"));

        assertThat(p.getDetail()).contains("recusou a requisicao");
    }

    // Mensagem nula acontece: nem toda excecao carrega texto. Quebrar aqui
    // transformaria uma falha do provedor num erro 500 sem explicacao.
    @Test
    void mensagemNulaNaoQuebraOTratamento() {
        ProblemDetail p = handler.provedorDeIaRecusou(new NonTransientAiException(null));

        assertThat(p.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
        assertThat(p.getDetail()).isNotBlank();
    }

    @Test
    void valorInvalidoViraBadRequest() {
        ProblemDetail p = handler.argumentoInvalido(
                new IllegalArgumentException("O valor da transacao deve ser positivo."));

        assertThat(p.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(p.getDetail()).contains("positivo");
    }

    @Test
    void recursoInexistenteViraNotFound() {
        ProblemDetail p = handler.naoEncontrado(
                new RecursoNaoEncontradoException("Nao existe transacao com o id 99."));

        assertThat(p.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(p.getDetail()).contains("99");
    }

    // Modelo local fora do ar e falha de conexao, nao erro de IA: sem este
    // tratamento viraria um 500 opaco.
    @Test
    void modeloLocalForaDoArViraServiceUnavailable() {
        ProblemDetail p = handler.provedorLocalForaDoAr(new ResourceAccessException("Connection refused"));

        assertThat(p.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(p.getDetail()).contains("ollama serve");
    }

    @Test
    void provedorInstavelViraServiceUnavailable() {
        ProblemDetail p = handler.provedorDeIaIndisponivel(new TransientAiException("503"));

        assertThat(p.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(p.getDetail()).contains("temporariamente");
    }
}
