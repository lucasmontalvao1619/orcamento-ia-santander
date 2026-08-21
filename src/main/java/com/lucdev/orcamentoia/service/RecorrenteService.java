package com.lucdev.orcamentoia.service;

import com.lucdev.orcamentoia.exception.RecursoNaoEncontradoException;
import com.lucdev.orcamentoia.model.Recorrente;
import com.lucdev.orcamentoia.model.TipoTransacao;
import com.lucdev.orcamentoia.model.Transacao;
import com.lucdev.orcamentoia.repository.RecorrenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class RecorrenteService {

    private final RecorrenteRepository repository;
    private final TransacaoService transacaoService;

    public RecorrenteService(RecorrenteRepository repository, TransacaoService transacaoService) {
        this.repository = repository;
        this.transacaoService = transacaoService;
    }

    @Transactional
    public Recorrente criar(String descricao, String categoria, TipoTransacao tipo,
                            BigDecimal valorPrevisto, Integer diaVencimento) {
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("A descricao e obrigatoria.");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("Informe se e um ganho ou um gasto fixo.");
        }
        // Previsao pode faltar — cartao de credito nao tem valor conhecido —,
        // mas quando existe precisa ser positiva, como qualquer valor do app.
        if (valorPrevisto != null && valorPrevisto.signum() <= 0) {
            throw new IllegalArgumentException("O valor previsto deve ser positivo.");
        }
        if (diaVencimento != null && (diaVencimento < 1 || diaVencimento > 31)) {
            throw new IllegalArgumentException("O dia deve estar entre 1 e 31.");
        }
        return repository.save(new Recorrente(descricao.trim(),
                categoria == null || categoria.isBlank() ? "outros" : categoria.trim(),
                tipo, valorPrevisto, diaVencimento));
    }

    @Transactional(readOnly = true)
    public List<Recorrente> listarTodos() {
        return repository.findAllByOrderByTipoAscDiaVencimentoAsc();
    }

    @Transactional(readOnly = true)
    public List<Recorrente> listarPorTipo(TipoTransacao tipo) {
        return repository.findAllByTipoOrderByDiaVencimentoAsc(tipo);
    }

    @Transactional(readOnly = true)
    public Recorrente obter(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Nao existe item fixo com o id " + id + "."));
    }

    @Transactional
    public Recorrente apagar(Long id) {
        Recorrente recorrente = obter(id);
        repository.delete(recorrente);
        return recorrente;
    }

    @Transactional
    public Recorrente atualizarPrevisao(Long id, BigDecimal valorPrevisto) {
        if (valorPrevisto != null && valorPrevisto.signum() <= 0) {
            throw new IllegalArgumentException("O valor previsto deve ser positivo.");
        }
        Recorrente recorrente = obter(id);
        recorrente.setValorPrevisto(valorPrevisto);
        return repository.save(recorrente);
    }

    // Registra a conta deste mes com o valor que REALMENTE veio.
    //
    // E aqui que contas variaveis — luz, agua, cartao — deixam de ser um
    // problema: o cadastro guarda a previsao, e o lancamento guarda a verdade.
    // Sem valor informado, usa a previsao; sem os dois, recusa, porque
    // adivinhar um valor poluiria o saldo com dinheiro inventado.
    @Transactional
    public Transacao lancar(Long id, BigDecimal valorReal) {
        Recorrente recorrente = obter(id);
        BigDecimal valor = valorReal != null ? valorReal : recorrente.getValorPrevisto();
        if (valor == null) {
            throw new IllegalArgumentException(
                    "Informe o valor de \"" + recorrente.getDescricao()
                            + "\": este item nao tem previsao cadastrada.");
        }
        return transacaoService.registrar(recorrente.getDescricao(), valor,
                recorrente.getCategoria(), recorrente.getTipo());
    }

    // Quanto se espera pagar ou receber por mes, somando as previsoes. Itens sem
    // previsao ficam de fora: entrar com zero daria uma falsa sensacao de que o
    // mes esta mais barato do que sera.
    @Transactional(readOnly = true)
    public BigDecimal totalPrevisto(TipoTransacao tipo) {
        return listarPorTipo(tipo).stream()
                .map(Recorrente::getValorPrevisto)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public long quantidadeSemPrevisao(TipoTransacao tipo) {
        return listarPorTipo(tipo).stream()
                .filter(r -> r.getValorPrevisto() == null)
                .count();
    }
}
