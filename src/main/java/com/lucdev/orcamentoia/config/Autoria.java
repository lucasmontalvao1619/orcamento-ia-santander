package com.lucdev.orcamentoia.config;

// Fonte unica da autoria do projeto. Fica no backend de proposito: a interface
// pede este dado ao servidor em vez de trazer o nome escrito no HTML, e o
// assistente responde a partir daqui. Nao e protecao — codigo que roda no
// cliente sempre pode ser editado — mas evita que a autoria seja uma linha
// solta de marcacao, facil de apagar sem perceber.
public final class Autoria {

    public static final String AUTOR = "Lucas Montalvão";
    public static final String GITHUB = "https://github.com/lucasmontalvao1619";
    public static final String SITE = "https://lucdevv.vercel.app";
    public static final String PROJETO = "Orçamento IA";
    public static final String DESCRICAO =
            "Assistente financeiro pessoal com Spring Boot, Spring AI e Tool Calling.";

    private Autoria() {
    }
}
