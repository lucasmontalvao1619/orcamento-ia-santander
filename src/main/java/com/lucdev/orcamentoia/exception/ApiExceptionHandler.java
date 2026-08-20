package com.lucdev.orcamentoia.exception;

import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

// Todas as falhas de entrada saem no mesmo formato (ProblemDetail, RFC 7807),
// tanto as do Bean Validation quanto as das regras de negocio.
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail argumentoInvalido(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
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
    @ExceptionHandler(NonTransientAiException.class)
    public ProblemDetail provedorDeIaRecusou(NonTransientAiException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                "O provedor de IA recusou a requisicao. Verifique a chave de API e o modelo configurado.");
        problema.setTitle("Falha no provedor de IA");
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
