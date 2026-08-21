package com.lucdev.orcamentoia.config;

import com.lucdev.orcamentoia.service.ConfiguracaoService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Entrega o cliente de chat que deve ser usado agora.
//
// O bean criado na inicializacao carrega a chave que existia naquele momento e
// nao muda mais. Como a chave e informada pela interface, depois que a
// aplicacao ja subiu, o cliente precisa ser montado na hora do uso — senao
// salvar a chave exigiria reiniciar o programa.
//
// O cliente montado fica guardado enquanto a chave nao mudar: reconstruir a
// cada mensagem criaria um cliente HTTP novo a cada comando, sem ganho nenhum.
@Component
public class ClienteDeChat {

    private final ChatClient clienteDaInicializacao;
    private final ConfiguracaoService configuracaoService;
    private final ChatMemory chatMemory;
    private final boolean memoriaAtiva;
    private final String modelo;

    private volatile String chaveDoCache;
    private volatile ChatClient clienteDoCache;

    public ClienteDeChat(ChatClient clienteDaInicializacao,
                         ConfiguracaoService configuracaoService,
                         ChatMemory chatMemory,
                         @Value("${assistente.memoria-ativa:false}") boolean memoriaAtiva,
                         @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}") String modelo) {
        this.clienteDaInicializacao = clienteDaInicializacao;
        this.configuracaoService = configuracaoService;
        this.chatMemory = chatMemory;
        this.memoriaAtiva = memoriaAtiva;
        this.modelo = modelo;
    }

    public ChatClient obter() {
        String chave = configuracaoService.obter().getChaveOpenAi();
        if (chave == null || chave.isBlank()) {
            return clienteDaInicializacao;
        }
        if (!chave.equals(chaveDoCache)) {
            clienteDoCache = montar(chave);
            chaveDoCache = chave;
        }
        return clienteDoCache;
    }

    private ChatClient montar(String chave) {
        OpenAiChatModel modeloDeChat = OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder().apiKey(chave).build())
                .defaultOptions(org.springframework.ai.openai.OpenAiChatOptions.builder()
                        .model(modelo)
                        .build())
                .build();

        ChatClient.Builder builder = ChatClient.builder(modeloDeChat)
                .defaultSystem(ChatClientConfig.promptDoSistema());

        if (memoriaAtiva) {
            builder = builder.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build());
        }
        return builder.build();
    }
}
