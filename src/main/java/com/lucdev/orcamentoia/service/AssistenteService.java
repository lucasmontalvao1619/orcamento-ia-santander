package com.lucdev.orcamentoia.service;

import com.lucdev.orcamentoia.tool.FinancasTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AssistenteService {

    private final ChatClient chatClient;
    private final FinancasTools financasTools;

    public AssistenteService(ChatClient chatClient, FinancasTools financasTools) {
        this.chatClient = chatClient;
        this.financasTools = financasTools;
    }

    public String processarComando(String textoUsuario) {
        return chatClient.prompt()
                .user(textoUsuario)
                .tools(financasTools)
                .call()
                .content();
    }
}
