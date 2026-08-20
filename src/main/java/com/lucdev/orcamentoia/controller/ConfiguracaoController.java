package com.lucdev.orcamentoia.controller;

import com.lucdev.orcamentoia.dto.CategoriaResponse;
import com.lucdev.orcamentoia.dto.CategoriasResponse;
import com.lucdev.orcamentoia.dto.ConfiguracaoResponse;
import com.lucdev.orcamentoia.dto.SalarioRequest;
import com.lucdev.orcamentoia.model.CategoriaDespesa;
import com.lucdev.orcamentoia.model.CategoriaReceita;
import com.lucdev.orcamentoia.service.ConfiguracaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ConfiguracaoController {

    private final ConfiguracaoService configuracaoService;

    public ConfiguracaoController(ConfiguracaoService configuracaoService) {
        this.configuracaoService = configuracaoService;
    }

    // A interface chama este endpoint ao abrir: quando configurado e false, ela
    // mostra a tela de boas-vindas pedindo o salario.
    @GetMapping("/configuracao")
    public ResponseEntity<ConfiguracaoResponse> obter() {
        return ResponseEntity.ok(ConfiguracaoResponse.de(configuracaoService.obter()));
    }

    @PutMapping("/configuracao/salario")
    public ResponseEntity<ConfiguracaoResponse> definirSalario(@RequestBody @Valid SalarioRequest request) {
        return ResponseEntity.ok(ConfiguracaoResponse.de(configuracaoService.definirSalario(request.salario())));
    }

    // Uma fonte unica de categorias para a interface, para que o formulario e o
    // rotulo exibido na tabela nao saiam de listas duplicadas no JavaScript.
    @GetMapping("/categorias")
    public ResponseEntity<CategoriasResponse> categorias() {
        List<CategoriaResponse> despesas = Arrays.stream(CategoriaDespesa.values())
                .map(c -> new CategoriaResponse(c.getValor(), c.getRotulo()))
                .toList();
        List<CategoriaResponse> receitas = Arrays.stream(CategoriaReceita.values())
                .map(c -> new CategoriaResponse(c.getValor(), c.getRotulo()))
                .toList();
        return ResponseEntity.ok(new CategoriasResponse(despesas, receitas));
    }
}
