package com.lucdev.orcamentoia.controller;

import com.lucdev.orcamentoia.service.ResumoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// A tabela de gastos por modalidade. Calculada a cada chamada, entao esta
// sempre em dia — nao ha nada guardado para ficar desatualizado.
@RestController
@RequestMapping("/api/resumo")
public class ResumoController {

    private final ResumoService servico;

    public ResumoController(ResumoService servico) {
        this.servico = servico;
    }

    @GetMapping
    public ResponseEntity<ResumoService.Resumo> resumo(
            @RequestParam(name = "mes", defaultValue = "true") boolean apenasMesAtual) {
        return ResponseEntity.ok(servico.montar(apenasMesAtual));
    }
}
