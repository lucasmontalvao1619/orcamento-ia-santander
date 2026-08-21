package com.lucdev.orcamentoia.controller;

import com.lucdev.orcamentoia.config.AmbienteDeExecucao;
import com.lucdev.orcamentoia.config.EncerramentoAutomatico;
import com.lucdev.orcamentoia.dto.SessaoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Liga a janela aberta ao processo: e por aqui que a interface avisa que ainda
// esta viva, e que descobre se deve fazer isso.
@RestController
@RequestMapping("/api/sessao")
public class SessaoController {

    private final EncerramentoAutomatico encerramento;
    private final AmbienteDeExecucao ambiente;

    public SessaoController(EncerramentoAutomatico encerramento, AmbienteDeExecucao ambiente) {
        this.encerramento = encerramento;
        this.ambiente = ambiente;
    }

    // A interface consulta antes de comecar a mandar sinais: no celular ou em
    // servidor nao ha janela para vigiar, e o trafego seria desperdicio.
    @GetMapping
    public ResponseEntity<SessaoResponse> estado() {
        return ResponseEntity.ok(new SessaoResponse(encerramento.isHabilitado(), ambiente.isEmContainer()));
    }

    @PostMapping("/sinal")
    public ResponseEntity<Void> sinalDeVida() {
        encerramento.registrarSinalDeVida();
        return ResponseEntity.noContent().build();
    }
}
