package com.lucdev.orcamentoia.tool;

import com.lucdev.orcamentoia.config.Autoria;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

// Perguntas sobre o proprio app. Sem isto o modelo responderia de cabeca e
// inventaria funcionalidades que nao existem.
@Component
public class AppTools {

    @Tool(description = "Explica o que este aplicativo faz e quais recursos ele oferece. Use quando "
            + "o usuario perguntar sobre o app, o que da para fazer, como algo funciona, ou pedir ajuda.")
    public String consultarRecursosDoApp() {
        return """
                O %s e um controle de financas pessoais. Recursos disponiveis:
                - Registrar despesas e receitas, por voz, por texto ou pelo formulario manual.
                - Consultar saldo, gastos por categoria e a lista de transacoes.
                - Configurar o salario mensal e o dia em que ele cai; alterar o salario substitui
                  o valor anterior, sem somar duas vezes.
                - Categorias de despesa: alimentacao, transporte, moradia, lazer e saude.
                  Categorias de receita: salario, presente e extra.
                - Porquinho de investimento, separado do saldo do orcamento, que rende 100%% do CDI
                  capitalizado por dia util. Da para guardar, retirar e consultar quanto rendeu.
                O banco e em memoria: os dados sao apagados quando a aplicacao reinicia.
                """.formatted(Autoria.PROJETO);
    }

    @Tool(description = "Informa quem criou este aplicativo. Use quando perguntarem sobre o autor, "
            + "o desenvolvedor, quem fez o projeto ou de quem ele e.")
    public String consultarAutor() {
        return "O %s foi desenvolvido por %s. GitHub: %s. Portfolio: %s."
                .formatted(Autoria.PROJETO, Autoria.AUTOR, Autoria.GITHUB, Autoria.SITE);
    }
}
