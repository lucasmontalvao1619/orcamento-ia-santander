package com.lucdev.orcamentoia.service;

import com.lucdev.orcamentoia.model.Investimento;
import com.lucdev.orcamentoia.model.TipoInvestimento;
import com.lucdev.orcamentoia.exception.RecursoNaoEncontradoException;
import com.lucdev.orcamentoia.repository.InvestimentoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class InvestimentoService {

    // O CDI e cotado ao ano mas rende por dia util, e o mercado usa 252 dias
    // uteis como ano padrao. Feriados sao ignorados aqui: incluir o calendario
    // bancario mudaria o resultado em centavos e exigiria manutencao anual.
    private static final int DIAS_UTEIS_NO_ANO = 252;
    private static final MathContext PRECISAO = new MathContext(20, RoundingMode.HALF_EVEN);

    private final InvestimentoRepository repository;
    private final BigDecimal cdiAnual;

    public InvestimentoService(InvestimentoRepository repository,
                               @Value("${investimentos.cdi-anual:0.1065}") BigDecimal cdiAnual) {
        this.repository = repository;
        this.cdiAnual = cdiAnual;
    }

    @Transactional
    public Investimento registrar(String descricao, BigDecimal valor, TipoInvestimento tipo) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("O valor do investimento deve ser positivo.");
        }
        // O limite considera o rendimento ja acumulado: como num banco, o que
        // rendeu tambem pode ser sacado.
        if (tipo == TipoInvestimento.RETIRADA) {
            BigDecimal disponivel = calcularSaldoComRendimento();
            if (valor.compareTo(disponivel) > 0) {
                throw new IllegalArgumentException(
                        "Nao e possivel retirar mais do que existe guardado. Disponivel: "
                                + disponivel.setScale(2, RoundingMode.HALF_EVEN) + ".");
            }
        }
        return repository.save(new Investimento(descricao, valor, tipo));
    }

    @Transactional
    public Investimento apagar(Long id) {
        Investimento investimento = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Nao existe movimento de investimento com o id " + id + "."));
        repository.delete(investimento);
        return investimento;
    }

    @Transactional(readOnly = true)
    public List<Investimento> listarTodos() {
        return repository.findAll();
    }

    // Quanto foi efetivamente depositado, sem rendimento.
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

    /**
     * Saldo com juros compostos a 100% do CDI, do jeito que um banco calcula:
     * percorre os movimentos em ordem e, entre um e outro, corrige o saldo pelos
     * dias uteis decorridos. O rendimento entra no saldo, entao ele proprio
     * passa a render — e por isso o calculo nao pode ser feito aporte a aporte.
     */
    @Transactional(readOnly = true)
    public BigDecimal calcularSaldoComRendimento() {
        List<Investimento> movimentos = repository.findAll().stream()
                .sorted(Comparator.comparing(Investimento::getDataHora))
                .toList();

        if (movimentos.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal saldo = BigDecimal.ZERO;
        LocalDateTime momento = movimentos.get(0).getDataHora();

        for (Investimento movimento : movimentos) {
            saldo = corrigir(saldo, momento, movimento.getDataHora());
            saldo = movimento.getTipo() == TipoInvestimento.APORTE
                    ? saldo.add(movimento.getValor())
                    : saldo.subtract(movimento.getValor());
            momento = movimento.getDataHora();
        }

        return corrigir(saldo, momento, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public BigDecimal calcularRendimento() {
        return calcularSaldoComRendimento().subtract(calcularTotal());
    }

    public BigDecimal getCdiAnual() {
        return cdiAnual;
    }

    private BigDecimal corrigir(BigDecimal saldo, LocalDateTime de, LocalDateTime ate) {
        if (saldo.signum() <= 0) {
            return saldo;
        }
        long dias = diasUteis(de.toLocalDate(), ate.toLocalDate());
        if (dias <= 0) {
            return saldo;
        }
        // (1 + CDI)^(dias/252): equivalente a capitalizar a taxa diaria a cada dia util.
        double fator = Math.pow(1 + cdiAnual.doubleValue(), (double) dias / DIAS_UTEIS_NO_ANO);
        return saldo.multiply(BigDecimal.valueOf(fator), PRECISAO);
    }

    private long diasUteis(LocalDate de, LocalDate ate) {
        if (!ate.isAfter(de)) {
            return 0;
        }
        long total = 0;
        for (LocalDate dia = de.plusDays(1); !dia.isAfter(ate); dia = dia.plusDays(1)) {
            DayOfWeek semana = dia.getDayOfWeek();
            if (semana != DayOfWeek.SATURDAY && semana != DayOfWeek.SUNDAY) {
                total++;
            }
        }
        return total;
    }
}
