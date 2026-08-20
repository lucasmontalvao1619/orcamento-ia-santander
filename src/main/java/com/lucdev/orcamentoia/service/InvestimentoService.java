package com.lucdev.orcamentoia.service;

import com.lucdev.orcamentoia.model.Investimento;
import com.lucdev.orcamentoia.model.TipoInvestimento;
import com.lucdev.orcamentoia.repository.InvestimentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class InvestimentoService {

    private final InvestimentoRepository repository;

    public InvestimentoService(InvestimentoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Investimento registrar(String descricao, BigDecimal valor, TipoInvestimento tipo) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("O valor do investimento deve ser positivo.");
        }
        // Retirar mais do que existe deixaria o porquinho negativo, o que nao
        // faz sentido para dinheiro guardado.
        if (tipo == TipoInvestimento.RETIRADA && valor.compareTo(calcularTotal()) > 0) {
            throw new IllegalArgumentException(
                    "Nao e possivel retirar mais do que existe guardado. Total atual: " + calcularTotal() + ".");
        }
        return repository.save(new Investimento(descricao, valor, tipo));
    }

    @Transactional(readOnly = true)
    public List<Investimento> listarTodos() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public BigDecimal calcularTotal() {
        return somar(TipoInvestimento.APORTE).subtract(somar(TipoInvestimento.RETIRADA));
    }

    @Transactional(readOnly = true)
    public BigDecimal somar(TipoInvestimento tipo) {
        return repository.findByTipo(tipo).stream()
                .map(Investimento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
