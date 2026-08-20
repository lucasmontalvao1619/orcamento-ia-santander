package com.lucdev.orcamentoia.service;

import com.lucdev.orcamentoia.model.CategoriaReceita;
import com.lucdev.orcamentoia.model.Configuracao;
import com.lucdev.orcamentoia.model.TipoTransacao;
import com.lucdev.orcamentoia.model.Transacao;
import com.lucdev.orcamentoia.repository.ConfiguracaoRepository;
import com.lucdev.orcamentoia.repository.TransacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ConfiguracaoService {

    private final ConfiguracaoRepository repository;
    private final TransacaoRepository transacaoRepository;

    public ConfiguracaoService(ConfiguracaoRepository repository, TransacaoRepository transacaoRepository) {
        this.repository = repository;
        this.transacaoRepository = transacaoRepository;
    }

    @Transactional(readOnly = true)
    public Configuracao obter() {
        return repository.findAll().stream().findFirst().orElseGet(Configuracao::new);
    }

    // Definir o salario cria a receita correspondente no primeiro acesso e, nas
    // vezes seguintes, atualiza a receita que ja existe. Sem isso, cada mudanca
    // de salario somaria de novo no saldo.
    @Transactional
    public Configuracao definirSalario(BigDecimal salario) {
        if (salario == null || salario.signum() <= 0) {
            throw new IllegalArgumentException("O salario deve ser positivo.");
        }

        Configuracao configuracao = repository.findAll().stream().findFirst().orElseGet(Configuracao::new);
        Transacao receita = buscarTransacaoSalario(configuracao);

        if (receita == null) {
            receita = new Transacao("Salário", salario, CategoriaReceita.SALARIO.getValor(), TipoTransacao.RECEITA);
        } else {
            receita.setValor(salario);
        }
        receita = transacaoRepository.save(receita);

        configuracao.setSalario(salario);
        configuracao.setTransacaoSalarioId(receita.getId());
        return repository.save(configuracao);
    }

    private Transacao buscarTransacaoSalario(Configuracao configuracao) {
        if (configuracao.getTransacaoSalarioId() == null) {
            return null;
        }
        // A transacao pode ter sido apagada por fora; nesse caso recriamos.
        return transacaoRepository.findById(configuracao.getTransacaoSalarioId()).orElse(null);
    }
}
