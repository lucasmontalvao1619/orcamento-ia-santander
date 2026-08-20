package com.lucdev.orcamentoia.controller;

import com.lucdev.orcamentoia.dto.AtualizarTransacaoRequest;
import com.lucdev.orcamentoia.dto.NovaTransacaoRequest;
import com.lucdev.orcamentoia.dto.TransacaoResponse;
import com.lucdev.orcamentoia.model.TipoTransacao;
import com.lucdev.orcamentoia.service.TransacaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/transacoes")
public class TransacaoController {

    private final TransacaoService transacaoService;

    public TransacaoController(TransacaoService transacaoService) {
        this.transacaoService = transacaoService;
    }

    @PostMapping
    public ResponseEntity<TransacaoResponse> criar(@RequestBody @Valid NovaTransacaoRequest request) {
        TransacaoResponse response = TransacaoResponse.de(transacaoService.criar(request));
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<TransacaoResponse>> listar(@RequestParam(required = false) TipoTransacao tipo) {
        List<TransacaoResponse> lista = (tipo == null
                ? transacaoService.listarTodas()
                : transacaoService.listarPorTipo(tipo))
                .stream()
                .map(TransacaoResponse::de)
                .toList();
        return ResponseEntity.ok(lista);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransacaoResponse> atualizar(@PathVariable Long id,
                                                       @RequestBody @Valid AtualizarTransacaoRequest request) {
        return ResponseEntity.ok(TransacaoResponse.de(transacaoService.atualizar(
                id, request.descricao(), request.valor(), request.categoria(), request.tipo())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> apagar(@PathVariable Long id) {
        transacaoService.apagar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/saldo")
    public ResponseEntity<BigDecimal> saldo() {
        return ResponseEntity.ok(transacaoService.calcularSaldo());
    }
}
