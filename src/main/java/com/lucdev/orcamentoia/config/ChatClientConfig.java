package com.lucdev.orcamentoia.config;

import com.lucdev.orcamentoia.model.CategoriaDespesa;
import com.lucdev.orcamentoia.model.CategoriaReceita;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
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

            Distinga configurar de registrar. "Meu salario e 3000", "estabelecer que
            eu recebo 3000 por mes" ou "todo dia 15 recebo 3000" configuram o salario:
            use definirSalario, que substitui o valor anterior. Ja "recebi 3000 de
            salario hoje" e uma entrada avulsa: use registrarTransacao. Na duvida
            entre as duas, pergunte antes de agir.

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

            NUNCA diga que registrou, atualizou ou apagou algo sem ter executado a
            ferramenta correspondente. Se faltar informacao para chamar a ferramenta,
            como o id de um lancamento, use listarTransacoes ou pergunte ao usuario —
            jamais descreva uma acao que voce nao realizou.

            Responda sempre em portugues, de forma curta, clara e amigavel.
            """;

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
    }

    /*
     * A memoria de conversa fica DESLIGADA por padrao, e isso e deliberado.
     *
     * Com historico, "gastei 50 no almoco" seguido de "na verdade foram 60"
     * passa a fazer sentido para o modelo. So que modelos locais menores, ao
     * verem no historico respostas antigas no formato "Transacao registrada
     * com sucesso...", passam a IMITAR esse texto em vez de chamar a
     * ferramenta: respondem "atualizado com sucesso" sem que nada mude no
     * banco. Foi medido: sem historico a ferramenta e chamada e o valor muda;
     * com historico o modelo so descreve o que faria.
     *
     * Um erro silencioso desses e pior que a falta do recurso, entao o padrao
     * e o comportamento correto. Com um modelo maior (AI_PROVIDER=openai, ou
     * um Ollama mais capaz) vale ligar: ASSISTENTE_MEMORIA=true.
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
                                 ChatMemory chatMemory,
                                 @Value("${assistente.memoria-ativa:false}") boolean memoriaAtiva) {
        String prompt = MODELO_PROMPT.formatted(
                listar(Arrays.stream(CategoriaDespesa.values()).map(CategoriaDespesa::getValor).toList()),
                listar(Arrays.stream(CategoriaReceita.values()).map(CategoriaReceita::getValor).toList()));

        ChatClient.Builder configurado = builder.defaultSystem(prompt);
        if (memoriaAtiva) {
            configurado = configurado.defaultAdvisors(
                    MessageChatMemoryAdvisor.builder(chatMemory).build());
        }
        return configurado.build();
    }

    private String listar(java.util.List<String> valores) {
        return valores.stream().collect(Collectors.joining(", "));
    }
}
