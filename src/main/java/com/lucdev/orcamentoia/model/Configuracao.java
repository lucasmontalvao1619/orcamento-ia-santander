package com.lucdev.orcamentoia.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigDecimal;

// Guarda o salario informado no primeiro acesso. E uma linha unica: o app tem
// um orcamento so, entao nao ha chave de usuario aqui.
@Entity
public class Configuracao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal salario;

    // Id da transacao de RECEITA que representa o salario. Guardar esse vinculo
    // evita duplicar o salario no saldo cada vez que ele e alterado: em vez de
    // criar outra receita, atualizamos a que ja existe.
    private Long transacaoSalarioId;

    // Dia do mes em que o salario cai. E uma anotacao do orcamento: nao existe
    // agendador criando lancamentos sozinho.
    private Integer diaRecebimento;

    public Configuracao() {
    }

    public boolean isConfigurado() {
        return salario != null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getSalario() {
        return salario;
    }

    public void setSalario(BigDecimal salario) {
        this.salario = salario;
    }

    public Integer getDiaRecebimento() {
        return diaRecebimento;
    }

    public void setDiaRecebimento(Integer diaRecebimento) {
        this.diaRecebimento = diaRecebimento;
    }

    public Long getTransacaoSalarioId() {
        return transacaoSalarioId;
    }

    public void setTransacaoSalarioId(Long transacaoSalarioId) {
        this.transacaoSalarioId = transacaoSalarioId;
    }
}
