package com.lucdev.orcamentoia.controller;

import com.lucdev.orcamentoia.dto.FecharMesRequest;
import com.lucdev.orcamentoia.dto.ItemDoMesResponse;
import com.lucdev.orcamentoia.dto.LancarRecorrenteRequest;
import com.lucdev.orcamentoia.dto.NovoRecorrenteRequest;
import com.lucdev.orcamentoia.dto.RecorrenteResponse;
import com.lucdev.orcamentoia.dto.TransacaoResponse;
import com.lucdev.orcamentoia.model.Transacao;
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
    // A tela de fechar o mes: tudo que se repete, com o que ja foi pago marcado.
    @GetMapping("/mes")
    public ResponseEntity<List<ItemDoMesResponse>> doMes() {
        return ResponseEntity.ok(servico.listarTodos().stream()
                .map(r -> {
                    var lancado = servico.lancamentoDoMes(r.getId());
                    return new ItemDoMesResponse(r.getId(), r.getDescricao(), r.getCategoria(),
                            r.getTipo(), r.getValorPrevisto(), r.getDiaVencimento(),
                            lancado.isPresent(),
                            lancado.map(Transacao::getValor).orElse(null));
                })
                .toList());
    }

    // Lanca de uma vez todas as contas informadas. O que ja foi lancado no mes
    // e pulado, para clicar duas vezes nao dobrar as contas.
    @PostMapping("/fechar-mes")
    public ResponseEntity<List<TransacaoResponse>> fecharMes(@RequestBody FecharMesRequest request) {
        return ResponseEntity.ok(servico.fecharMes(
                        request.valores() == null ? java.util.Map.of() : request.valores()).stream()
                .map(TransacaoResponse::de)
                .toList());
    }

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
