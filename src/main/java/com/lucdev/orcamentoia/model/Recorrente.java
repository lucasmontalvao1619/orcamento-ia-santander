package com.lucdev.orcamentoia.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigDecimal;

// Um ganho ou gasto que se repete todo mes: aluguel, luz, agua, internet,
// cartao, mensalidade, uma renda extra fixa.
//
// O valor aqui e uma PREVISAO, nao um lancamento. Conta de luz, agua e cartao
// mudam todo mes: guardar um valor fixo obrigaria o usuario a corrigir o
// cadastro toda vez, ou o orcamento mentiria. O valor real entra quando a conta
// e lancada, e a previsao serve para saber o que esta por vir.
@Entity
public class Recorrente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String descricao;

    private String categoria;

    @Enumerated(EnumType.STRING)
    private TipoTransacao tipo;

    // Opcional de proposito: quem nao faz ideia de quanto vem — cartao de
    // credito, tipicamente — cadastra sem previsao e informa so ao lancar.
    private BigDecimal valorPrevisto;

    // Dia do mes em que vence ou cai. Anotacao do orcamento: nao existe
    // agendador criando lancamento sozinho, porque um lancamento automatico com
    // valor previsto errado poluiria o saldo com dinheiro que nao existe.
    private Integer diaVencimento;

    public Recorrente() {
    }

    public Recorrente(String descricao, String categoria, TipoTransacao tipo,
                      BigDecimal valorPrevisto, Integer diaVencimento) {
        this.descricao = descricao;
        this.categoria = categoria;
        this.tipo = tipo;
        this.valorPrevisto = valorPrevisto;
        this.diaVencimento = diaVencimento;
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

    public BigDecimal getValorPrevisto() {
        return valorPrevisto;
    }

    public void setValorPrevisto(BigDecimal valorPrevisto) {
        this.valorPrevisto = valorPrevisto;
    }

    public Integer getDiaVencimento() {
        return diaVencimento;
    }

    public void setDiaVencimento(Integer diaVencimento) {
        this.diaVencimento = diaVencimento;
    }
}
