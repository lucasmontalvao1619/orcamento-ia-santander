package com.lucdev.orcamentoia.service;

import com.lucdev.orcamentoia.dto.NovaTransacaoRequest;
import com.lucdev.orcamentoia.model.TipoTransacao;
import com.lucdev.orcamentoia.model.Transacao;
import com.lucdev.orcamentoia.repository.TransacaoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransacaoService {

    private final TransacaoRepository repository;

    public TransacaoService(TransacaoRepository repository) {
        this.repository = repository;
    }

    public Transacao criar(NovaTransacaoRequest request) {
        Transacao transacao = new Transacao(
                request.descricao(),
                request.valor(),
                request.categoria(),
                request.tipo()
        );
        return repository.save(transacao);
    }

    public Transacao registrar(String descricao, BigDecimal valor, String categoria, TipoTransacao tipo) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("O valor da transacao deve ser positivo.");
        }
        Transacao transacao = new Transacao(descricao, valor, categoria, tipo);
        return repository.save(transacao);
    }

    public List<Transacao> listarTodas() {
        return repository.findAll();
    }

    public List<Transacao> listarPorTipo(TipoTransacao tipo) {
        return repository.findByTipo(tipo);
    }

    public List<Transacao> listarPorCategoria(String categoria) {
        return repository.findByCategoriaIgnoreCase(categoria);
    }

    public BigDecimal calcularSaldo() {
        BigDecimal receitas = somar(TipoTransacao.RECEITA);
        BigDecimal despesas = somar(TipoTransacao.DESPESA);
        return receitas.subtract(despesas);
    }

    public BigDecimal somar(TipoTransacao tipo) {
        return repository.findByTipo(tipo).stream()
                .map(Transacao::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
