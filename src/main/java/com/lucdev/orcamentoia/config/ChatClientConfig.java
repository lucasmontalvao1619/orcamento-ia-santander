package com.lucdev.orcamentoia.config;

import com.lucdev.orcamentoia.model.CategoriaDespesa;
import com.lucdev.orcamentoia.model.CategoriaReceita;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.stream.Collectors;

@Configuration
public class ChatClientConfig {

    // As regras sao explicitas porque modelos locais menores tendem a inventar um
    // resumo proprio depois do tool call, chegando a citar valores que nao vieram
    // da ferramenta, ou a escrever a chamada da ferramenta como texto.
    // As categorias saem dos enums para nao existir uma segunda lista aqui que
    // pudesse divergir da que a interface usa.
    private static final String MODELO_PROMPT = """
            Voce e um assistente financeiro pessoal. Sua funcao e ajudar o usuario a
            registrar e consultar transacoes do orcamento dele.

            Interprete o comando do usuario e use as ferramentas disponiveis para
            registrar receitas e despesas ou consultar saldo, gastos e transacoes.

            Ao registrar uma transacao, extraia do texto a descricao, o valor, a
            categoria e o tipo. A descricao deve ser curta: o nome do item ou do
            servico, nao a frase inteira do usuario. Se alguma informacao essencial
            estiver faltando, pergunte de forma objetiva antes de registrar.

            Use sempre uma destas categorias:
            - Despesas: %s
            - Receitas: %s
            Dinheiro recebido de presente usa a categoria presente. Trabalho avulso
            ou renda fora do salario usa a categoria extra.

            REGRA OBRIGATORIA: depois de usar uma ferramenta, sua resposta deve ser
            exatamente o texto que a ferramenta retornou. Nao reescreva, nao resuma e
            nao acrescente comentarios. Nunca invente valores, saldos ou datas: todo
            numero que voce mostrar tem de ter vindo do retorno de uma ferramenta.
            Nunca escreva chamadas de ferramenta como texto na resposta, e nunca use
            as palavras "ferramenta", "tool" ou "output".

            Responda sempre em portugues, de forma curta, clara e amigavel.
            """;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        String prompt = MODELO_PROMPT.formatted(
                listar(Arrays.stream(CategoriaDespesa.values()).map(CategoriaDespesa::getValor).toList()),
                listar(Arrays.stream(CategoriaReceita.values()).map(CategoriaReceita::getValor).toList()));

        return builder
                .defaultSystem(prompt)
                .build();
    }

    private String listar(java.util.List<String> valores) {
        return valores.stream().collect(Collectors.joining(", "));
    }
}
