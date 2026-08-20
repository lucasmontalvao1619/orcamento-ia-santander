package com.lucdev.orcamentoia.model;

// As cinco categorias de despesa oferecidas na interface. O campo categoria da
// Transacao continua sendo texto livre porque a IA pode inventar categorias ao
// registrar por voz; este enum define apenas as opcoes fixas do formulario.
public enum CategoriaDespesa {

    ALIMENTACAO("alimentacao", "Alimentação"),
    TRANSPORTE("transporte", "Transporte"),
    MORADIA("moradia", "Moradia"),
    LAZER("lazer", "Lazer"),
    SAUDE("saude", "Saúde");

    private final String valor;
    private final String rotulo;

    CategoriaDespesa(String valor, String rotulo) {
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
