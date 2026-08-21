package com.lucdev.orcamentoia.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String descricao;

    private BigDecimal valor;

    private String categoria;

    @Enumerated(EnumType.STRING)
    private TipoTransacao tipo;

    private LocalDateTime dataHora;

    // De qual item fixo este lancamento veio, quando veio de um.
    //
    // Sem esse vinculo, saber se a conta de luz ja foi paga neste mes exigiria
    // comparar descricao e categoria — e um lancamento manual com o mesmo nome
    // passaria por conta paga. Fica nulo em lancamento avulso, entao a coluna e
    // opcional: coluna NOT NULL nova quebraria a atualizacao de quem ja usa o
    // aplicativo.
    private Long recorrenteId;

    // De qual movimento do porquinho este lancamento veio, quando veio de um.
    // Guardar dinheiro sai da conta e retirar volta para ela, entao cada
    // movimento tem um lancamento correspondente. Opcional, como recorrenteId:
    // coluna NOT NULL nova quebraria a atualizacao de quem ja usa o app.
    private Long investimentoId;

    public Transacao() {
        this.dataHora = LocalDateTime.now();
    }

    public Transacao(String descricao, BigDecimal valor, String categoria, TipoTransacao tipo) {
        this.descricao = descricao;
        this.valor = valor;
        this.categoria = categoria;
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

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public TipoTransacao getTipo() {
        return tipo;
    }

    public void setTipo(TipoTransacao tipo) {
        this.tipo = tipo;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public Long getRecorrenteId() {
        return recorrenteId;
    }

    public void setRecorrenteId(Long recorrenteId) {
        this.recorrenteId = recorrenteId;
    }

    public Long getInvestimentoId() {
        return investimentoId;
    }

    public void setInvestimentoId(Long investimentoId) {
        this.investimentoId = investimentoId;
    }
}
