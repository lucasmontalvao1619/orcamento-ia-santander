package com.lucdev.orcamentoia.service;

import com.lucdev.orcamentoia.tool.AppTools;
import com.lucdev.orcamentoia.tool.FinancasTools;
import com.lucdev.orcamentoia.tool.InvestimentoTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.lucdev.orcamentoia.config.ClienteDeChat;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.retry.NonTransientAiException;
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

    private final InterpretadorDeComandos interpretador;
    private final ConfiguracaoService configuracaoService;
    private final ClienteDeChat clienteDeChat;
    private final FinancasTools financasTools;
    private final InvestimentoTools investimentoTools;
    private final AppTools appTools;

    public AssistenteService(InterpretadorDeComandos interpretador,
                             ConfiguracaoService configuracaoService,
                             ClienteDeChat clienteDeChat,
                             FinancasTools financasTools,
                             InvestimentoTools investimentoTools,
                             AppTools appTools) {
        this.interpretador = interpretador;
        this.configuracaoService = configuracaoService;
        this.clienteDeChat = clienteDeChat;
        this.financasTools = financasTools;
        this.investimentoTools = investimentoTools;
        this.appTools = appTools;
    }

    // Mostrada quando o interpretador nao reconhece a frase. Lista exemplos
    // reais em vez de um "nao entendi" seco: sem IA, o usuario precisa saber
    // qual formato funciona.
    private static final String NAO_ENTENDI =
            "Nao entendi esse comando.\n\n" + InterpretadorDeComandos.AJUDA
            + "\n\nPara frases livres, informe sua chave da OpenAI em Configuracoes.";

    // Anexado a resposta quando a IA falhou e o interpretador salvou o comando:
    // sem isto, o usuario acharia que a chave esta funcionando.
    //
    // O motivo vem do erro real: conta sem credito e chave invalida pedem acoes
    // opostas, e chutar "sem credito" mandaria quem digitou a chave errada
    // procurar no lugar errado.
    private static String avisoDeFallback(String causa) {
        String motivo;
        if (causa != null && causa.contains("insufficient_quota")) {
            motivo = "conta sem credito";
        } else if (causa != null && (causa.contains("invalid_api_key") || causa.contains("Incorrect API key"))) {
            motivo = "chave recusada";
        } else {
            motivo = "falha na chamada";
        }
        return "\n\n(A OpenAI nao respondeu — " + motivo
                + ". Este comando foi atendido pelo interpretador local.)";
    }

    public String processarComando(String textoUsuario) {
        // Sem chave da OpenAI nao ha modelo para interpretar a frase, mas o
        // aplicativo nao pode ficar mudo: o interpretador proprio entende os
        // comandos comuns de graca, offline e na hora. Com chave, a IA assume e
        // lida tambem com frases fora do padrao.
        if (!configuracaoService.obter().temChaveOpenAi()) {
            return interpretador.interpretar(textoUsuario).orElse(NAO_ENTENDI);
        }

        try {
            String resposta = clienteDeChat.obter().prompt()
                    .user(textoUsuario)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, CONVERSA))
                    .tools(financasTools, investimentoTools, appTools)
                    .call()
                    .content();

            return sanitizar(resposta);
        } catch (NonTransientAiException e) {
            // A chave existe mas o provedor recusou — tipicamente conta sem
            // credito. Deixar o comando morrer aqui seria pior do que atender
            // pelo interpretador: o usuario configurou a chave justamente para
            // usar o assistente, e o app sabe executar isto sozinho.
            log.warn("Provedor de IA recusou; usando o interpretador proprio.", e);
            return interpretador.interpretar(textoUsuario)
                    .map(r -> r + avisoDeFallback(e.getMessage()))
                    .orElseThrow(() -> e);
        }
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
