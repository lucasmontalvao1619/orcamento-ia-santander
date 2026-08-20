package com.lucdev.orcamentoia.service;

import com.lucdev.orcamentoia.dto.NovaTransacaoRequest;
import com.lucdev.orcamentoia.model.TipoTransacao;
import com.lucdev.orcamentoia.model.Transacao;
import com.lucdev.orcamentoia.exception.RecursoNaoEncontradoException;
import com.lucdev.orcamentoia.repository.TransacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransacaoService {

    private final TransacaoRepository repository;

    public TransacaoService(TransacaoRepository repository) {
        this.repository = repository;
    }

    // A entrada REST e a entrada da IA caem no mesmo metodo para que a regra de
    // valor positivo valha nos dois caminhos, e nao so onde o @Valid atua.
    @Transactional
    public Transacao criar(NovaTransacaoRequest request) {
        return registrar(request.descricao(), request.valor(), request.categoria(), request.tipo());
    }

    @Transactional
    public Transacao registrar(String descricao, BigDecimal valor, String categoria, TipoTransacao tipo) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("O valor da transacao deve ser positivo.");
        }
        Transacao transacao = new Transacao(descricao, valor, categoria, tipo);
        return repository.save(transacao);
    }

    // Atualizacao parcial: campo nulo significa "mantem o que estava". Sem isso,
    // corrigir so o valor exigiria reenviar descricao, categoria e tipo.
    @Transactional
    public Transacao atualizar(Long id, String descricao, BigDecimal valor, String categoria, TipoTransacao tipo) {
        Transacao transacao = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Nao existe transacao com o id " + id + "."));

        if (valor != null) {
            if (valor.signum() <= 0) {
                throw new IllegalArgumentException("O valor da transacao deve ser positivo.");
            }
            transacao.setValor(valor);
        }
        if (descricao != null && !descricao.isBlank()) {
            transacao.setDescricao(descricao);
        }
        if (categoria != null && !categoria.isBlank()) {
            transacao.setCategoria(categoria);
        }
        if (tipo != null) {
            transacao.setTipo(tipo);
        }
        return repository.save(transacao);
    }

    @Transactional
    public Transacao apagar(Long id) {
        Transacao transacao = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Nao existe transacao com o id " + id + "."));
        repository.delete(transacao);
        return transacao;
    }

    @Transactional(readOnly = true)
    public List<Transacao> listarTodas() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Transacao> listarPorTipo(TipoTransacao tipo) {
        return repository.findByTipo(tipo);
    }

    @Transactional(readOnly = true)
    public List<Transacao> listarPorCategoria(String categoria) {
        return repository.findByCategoriaIgnoreCase(categoria);
    }

    @Transactional(readOnly = true)
    public BigDecimal calcularSaldo() {
        return somar(TipoTransacao.RECEITA).subtract(somar(TipoTransacao.DESPESA));
    }

    @Transactional(readOnly = true)
    public BigDecimal somar(TipoTransacao tipo) {
        return repository.findByTipo(tipo).stream()
                .map(Transacao::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
