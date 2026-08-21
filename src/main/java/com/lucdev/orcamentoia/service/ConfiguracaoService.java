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
        return definirSalario(salario, null);
    }

    @Transactional
    public Configuracao definirSalario(BigDecimal salario, Integer diaRecebimento) {
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
        configuracao.setSemSalario(false);
        configuracao.setTransacaoSalarioId(receita.getId());
        if (diaRecebimento != null) {
            if (diaRecebimento < 1 || diaRecebimento > 31) {
                throw new IllegalArgumentException("O dia do recebimento deve estar entre 1 e 31.");
            }
            configuracao.setDiaRecebimento(diaRecebimento);
        }
        return repository.save(configuracao);
    }

    // Para quem vive de renda variavel. Sem isto, a unica forma de sair da tela
    // de boas-vindas seria inventar um salario que a pessoa nao tem — e esse
    // valor entraria no saldo como receita, mentindo sobre quanto ela tem.
    @Transactional
    public Configuracao declararQueNaoTemSalario() {
        Configuracao configuracao = repository.findAll().stream().findFirst().orElseGet(Configuracao::new);

        // Se ja havia um salario, a receita dele precisa sair junto: deixa-la no
        // orcamento manteria no saldo um dinheiro que a pessoa acabou de dizer
        // que nao recebe.
        Transacao receita = buscarTransacaoSalario(configuracao);
        if (receita != null) {
            transacaoRepository.delete(receita);
        }

        configuracao.setSalario(null);
        configuracao.setTransacaoSalarioId(null);
        configuracao.setDiaRecebimento(null);
        configuracao.setSemSalario(true);
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
