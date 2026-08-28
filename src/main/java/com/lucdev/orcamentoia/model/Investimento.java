package com.lucdev.orcamentoia.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Um movimento do porquinho. E um registro separado das transacoes do orcamento:
// o dinheiro guardado tem total proprio e nao entra no saldo do dia a dia.
@Entity
public class Investimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String descricao;

    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    private TipoInvestimento tipo;

    private LocalDateTime dataHora;

    public Investimento() {
        this.dataHora = LocalDateTime.now();
    }

    public Investimento(String descricao, BigDecimal valor, TipoInvestimento tipo) {
        this.descricao = descricao;
        this.valor = valor;
        this.tipo = tipo;
        this.dataHora = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public TipoInvestimento getTipo() {
        return tipo;
    }

    public void setTipo(TipoInvestimento tipo) {
        this.tipo = tipo;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }
}
