package com.lucdev.orcamentoia.controller;

import com.lucdev.orcamentoia.dto.InvestimentoResponse;
import com.lucdev.orcamentoia.dto.NovoInvestimentoRequest;
import com.lucdev.orcamentoia.dto.ResumoInvestimentoResponse;
import com.lucdev.orcamentoia.model.TipoInvestimento;
import com.lucdev.orcamentoia.service.InvestimentoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// O porquinho e um controle a parte: total proprio, sem misturar com o saldo do
// orcamento. Guardar dinheiro aqui e uma anotacao, nao um lancamento do mes.
@RestController
@RequestMapping("/api/investimentos")
public class InvestimentoController {

    private final InvestimentoService investimentoService;

    public InvestimentoController(InvestimentoService investimentoService) {
        this.investimentoService = investimentoService;
    }

    @PostMapping
    public ResponseEntity<InvestimentoResponse> criar(@RequestBody @Valid NovoInvestimentoRequest request) {
        return ResponseEntity.ok(InvestimentoResponse.de(
                investimentoService.registrar(request.descricao(), request.valor(), request.tipo())));
    }

    @GetMapping
    public ResponseEntity<List<InvestimentoResponse>> listar() {
        return ResponseEntity.ok(investimentoService.listarTodos().stream()
                .map(InvestimentoResponse::de)
                .toList());
    }

    @GetMapping("/resumo")
    public ResponseEntity<ResumoInvestimentoResponse> resumo() {
        return ResponseEntity.ok(new ResumoInvestimentoResponse(
                investimentoService.calcularTotal(),
                investimentoService.somar(TipoInvestimento.APORTE),
                investimentoService.somar(TipoInvestimento.RETIRADA)));
    }
}
