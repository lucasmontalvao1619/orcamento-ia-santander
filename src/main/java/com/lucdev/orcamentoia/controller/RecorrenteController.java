package com.lucdev.orcamentoia.controller;

import com.lucdev.orcamentoia.dto.LancarRecorrenteRequest;
import com.lucdev.orcamentoia.dto.NovoRecorrenteRequest;
import com.lucdev.orcamentoia.dto.RecorrenteResponse;
import com.lucdev.orcamentoia.dto.TransacaoResponse;
import com.lucdev.orcamentoia.service.RecorrenteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Ganhos e gastos que se repetem todo mes. O cadastro guarda a previsao; o
// lancamento guarda o valor real, que e como contas variaveis funcionam.
@RestController
@RequestMapping("/api/fixos")
public class RecorrenteController {

    private final RecorrenteService servico;

    public RecorrenteController(RecorrenteService servico) {
        this.servico = servico;
    }

    @PostMapping
    public ResponseEntity<RecorrenteResponse> criar(@RequestBody @Valid NovoRecorrenteRequest request) {
        return ResponseEntity.ok(RecorrenteResponse.de(servico.criar(
                request.descricao(), request.categoria(), request.tipo(),
                request.valorPrevisto(), request.diaVencimento())));
    }

    @GetMapping
    public ResponseEntity<List<RecorrenteResponse>> listar() {
        return ResponseEntity.ok(servico.listarTodos().stream()
                .map(RecorrenteResponse::de)
                .toList());
    }

    // O valor do corpo e o que realmente veio na conta. Vazio usa a previsao.
    @PostMapping("/{id}/lancar")
    public ResponseEntity<TransacaoResponse> lancar(@PathVariable Long id,
                                                    @RequestBody(required = false) @Valid LancarRecorrenteRequest request) {
        return ResponseEntity.ok(TransacaoResponse.de(
                servico.lancar(id, request == null ? null : request.valor())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> apagar(@PathVariable Long id) {
        servico.apagar(id);
        return ResponseEntity.noContent().build();
    }
}
