package com.lucdev.orcamentoia.service;

import com.lucdev.orcamentoia.tool.FinancasTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class AssistenteService {

    private static final Logger log = LoggerFactory.getLogger(AssistenteService.class);

    // Modelos locais menores as vezes escrevem a chamada de ferramenta como
    // texto em vez de emitir um tool call de verdade. Quando isso acontece a
    // ferramenta ate roda, mas a resposta que chegaria ao usuario seria um JSON
    // cru. Este padrao detecta esse vazamento para nao mostra-lo na tela.
    private static final Pattern VAZAMENTO_TOOL_CALL = Pattern.compile(
            "\\{\\s*\"(name|function|tool|parameters|arguments)\"\\s*:", Pattern.CASE_INSENSITIVE);

    private static final String RESPOSTA_PADRAO =
            "Pronto, comando processado. Confira o saldo e a lista de transacoes.";

    private final ChatClient chatClient;
    private final FinancasTools financasTools;

    public AssistenteService(ChatClient chatClient, FinancasTools financasTools) {
        this.chatClient = chatClient;
        this.financasTools = financasTools;
    }

    public String processarComando(String textoUsuario) {
        String resposta = chatClient.prompt()
                .user(textoUsuario)
                .tools(financasTools)
                .call()
                .content();

        return sanitizar(resposta);
    }

    private String sanitizar(String resposta) {
        if (resposta == null || resposta.isBlank()) {
            return RESPOSTA_PADRAO;
        }
        if (VAZAMENTO_TOOL_CALL.matcher(resposta).find()) {
            log.warn("O modelo devolveu uma chamada de ferramenta como texto; usando resposta padrao.");
            return RESPOSTA_PADRAO;
        }
        return resposta.trim();
    }
}
