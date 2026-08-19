package com.lucdev.orcamentoia.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    private static final String SYSTEM_PROMPT = """
            Voce e um assistente financeiro pessoal. Sua funcao e ajudar o usuario a
            registrar e consultar transacoes do orcamento dele.

            Interprete o comando do usuario e use as ferramentas disponiveis para
            registrar receitas e despesas ou consultar saldo, gastos e transacoes.

            Ao registrar uma despesa ou receita, extraia do texto a descricao, o valor,
            a categoria e o tipo. Se alguma informacao essencial estiver faltando,
            pergunte de forma objetiva antes de registrar.

            Responda sempre em portugues, de forma curta, clara e amigavel.
            """;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }
}
