package com.lucdev.orcamentoia.model;

import jakarta.persistence.Column;
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

    // Quem vive de renda variavel — autonomo, freelancer, quem ainda nao
    // trabalha — nao tem um valor mensal para informar, e sem esta marca ficaria
    // preso na tela de boas-vindas, ja que "configurado" significava ter
    // salario. Aqui a pessoa declara que nao tem, e lanca cada entrada conforme
    // o dinheiro chega.
    // O default na definicao da coluna nao e decoracao: sem ele, o Hibernate
    // gera "add column sem_salario boolean not null" e a atualizacao quebra em
    // qualquer instalacao que ja tenha uma linha de configuracao — as linhas
    // existentes ficariam com NULL numa coluna que nao aceita NULL.
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean semSalario;

    // Chave da OpenAI informada pela interface, para quem quiser transcricao de
    // voz no servidor (Whisper) em vez do reconhecimento do navegador.
    //
    // Fica em texto puro no banco local, dentro da pasta pessoal do usuario.
    // Nao ha servidor nem outros usuarios: quem alcanca este arquivo ja esta na
    // conta da pessoa. Ainda assim, a interface nunca devolve a chave de volta —
    // so informa se existe uma configurada.
    @Column(length = 200)
    private String chaveOpenAi;

    public Configuracao() {
    }

    public boolean isConfigurado() {
        return salario != null || semSalario;
    }

    public String getChaveOpenAi() {
        return chaveOpenAi;
    }

    public void setChaveOpenAi(String chaveOpenAi) {
        this.chaveOpenAi = chaveOpenAi;
    }

    public boolean temChaveOpenAi() {
        return chaveOpenAi != null && !chaveOpenAi.isBlank();
    }

    public boolean isSemSalario() {
        return semSalario;
    }

    public void setSemSalario(boolean semSalario) {
        this.semSalario = semSalario;
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
