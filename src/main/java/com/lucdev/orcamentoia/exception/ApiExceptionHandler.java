package com.lucdev.orcamentoia.exception;

import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

import java.util.stream.Collectors;

// Todas as falhas de entrada saem no mesmo formato (ProblemDetail, RFC 7807),
// tanto as do Bean Validation quanto as das regras de negocio.
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail argumentoInvalido(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail naoEncontrado(RecursoNaoEncontradoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail corpoInvalido(MethodArgumentNotValidException e) {
        String detalhe = e.getBindingResult().getFieldErrors().stream()
                .map(erro -> "%s %s".formatted(erro.getField(), erro.getDefaultMessage()))
                .collect(Collectors.joining("; "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalhe);
    }

    // Falha do provedor de IA nao e erro da nossa aplicacao: sem estes handlers
    // uma chave invalida ou uma indisponibilidade da OpenAI vira um 500 opaco.
    // "Recusou a requisicao" nao diz o que fazer. As duas causas reais pedem
    // acoes opostas: chave sem credito e problema de cobranca, chave invalida e
    // problema de configuracao. Distinguir poupa o usuario de procurar no lugar
    // errado — foi o que aconteceu num teste real, com a chave certa e a conta
    // sem credito.
    @ExceptionHandler(NonTransientAiException.class)
    public ProblemDetail provedorDeIaRecusou(NonTransientAiException e) {
        String causa = e.getMessage() == null ? "" : e.getMessage();
        String detalhe;
        if (causa.contains("insufficient_quota")) {
            detalhe = "Sua conta da OpenAI esta sem credito. A chave e valida, mas cada resposta "
                    + "consome credito: adicione saldo em platform.openai.com/billing.";
        } else if (causa.contains("invalid_api_key") || causa.contains("Incorrect API key")) {
            detalhe = "A chave da OpenAI foi recusada. Confira se copiou a chave inteira em "
                    + "Configuracoes, sem espacos.";
        } else {
            detalhe = "O provedor de IA recusou a requisicao. Verifique a chave de API e o modelo configurado.";
        }
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, detalhe);
        problema.setTitle("Falha no provedor de IA");
        return problema;
    }

    // O Ollama roda como processo separado. Se ele nao estiver no ar, a chamada
    // falha na conexao e nao como erro de IA, entao precisa do proprio handler.
    @ExceptionHandler(ResourceAccessException.class)
    public ProblemDetail provedorLocalForaDoAr(ResourceAccessException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Nao foi possivel conectar ao modelo local. Verifique se o Ollama esta rodando (ollama serve).");
        problema.setTitle("Modelo local indisponivel");
        return problema;
    }

    @ExceptionHandler(TransientAiException.class)
    public ProblemDetail provedorDeIaIndisponivel(TransientAiException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "O provedor de IA esta temporariamente indisponivel. Tente novamente em instantes.");
        problema.setTitle("Provedor de IA indisponivel");
        return problema;
    }
}
