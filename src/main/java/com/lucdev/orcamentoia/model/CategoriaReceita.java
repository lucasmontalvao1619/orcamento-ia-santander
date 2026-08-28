package com.lucdev.orcamentoia.model;

// Categorias de entrada de dinheiro. PRESENTE e EXTRA cobrem o dinheiro que
// chega fora do salario, como um presente ou um trabalho avulso.
public enum CategoriaReceita {

    SALARIO("salario", "Salário"),
    PRESENTE("presente", "Presente"),
    EXTRA("extra", "Renda extra");

    private final String valor;
    private final String rotulo;

    CategoriaReceita(String valor, String rotulo) {
        this.valor = valor;
        this.rotulo = rotulo;
    }

    public String getValor() {
        return valor;
    }

    public String getRotulo() {
        return rotulo;
    }
}
