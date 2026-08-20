package com.lucdev.orcamentoia.service;

import com.lucdev.orcamentoia.tool.AppTools;
import com.lucdev.orcamentoia.tool.FinancasTools;
import com.lucdev.orcamentoia.tool.InvestimentoTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
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

    // O app tem um orcamento so, entao ha uma conversa so. Quando existir a
    // nocao de usuario, este id passa a vir de quem esta autenticado.
    private static final String CONVERSA = "orcamento-unico";

    private static final String RESPOSTA_PADRAO =
            "Pronto, comando processado. Confira o saldo e a lista de transacoes.";

    private final ChatClient chatClient;
    private final FinancasTools financasTools;
    private final InvestimentoTools investimentoTools;
    private final AppTools appTools;

    public AssistenteService(ChatClient chatClient,
                             FinancasTools financasTools,
                             InvestimentoTools investimentoTools,
                             AppTools appTools) {
        this.chatClient = chatClient;
        this.financasTools = financasTools;
        this.investimentoTools = investimentoTools;
        this.appTools = appTools;
    }

    public String processarComando(String textoUsuario) {
        String resposta = chatClient.prompt()
                .user(textoUsuario)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, CONVERSA))
                .tools(financasTools, investimentoTools, appTools)
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
