package com.lucdev.orcamentoia.controller;

import com.lucdev.orcamentoia.dto.ComandoResponse;
import com.lucdev.orcamentoia.dto.StatusResponse;
import com.lucdev.orcamentoia.service.AssistenteService;
import com.lucdev.orcamentoia.service.ConfiguracaoService;
import com.lucdev.orcamentoia.service.TranscricaoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/assistente")
public class AssistenteController {

    // Valor usado como default quando a chave nao esta no ambiente (ver application.properties).
    private static final String CHAVE_AUSENTE = "chave-nao-configurada";

    private final TranscricaoService transcricaoService;
    private final AssistenteService assistenteService;
    private final ConfiguracaoService configuracaoService;
    private final String provedor;
    private final String chaveDoAmbiente;

    public AssistenteController(TranscricaoService transcricaoService,
                                AssistenteService assistenteService,
                                ConfiguracaoService configuracaoService,
                                @Value("${spring.ai.model.chat:openai}") String provedor,
                                @Value("${spring.ai.openai.api-key:}") String chaveDoAmbiente) {
        this.transcricaoService = transcricaoService;
        this.assistenteService = assistenteService;
        this.configuracaoService = configuracaoService;
        this.provedor = provedor;
        this.chaveDoAmbiente = chaveDoAmbiente;
    }

    // A interface chama este endpoint ao abrir. Sem ele o usuario so descobriria
    // que falta configuracao depois de mandar um comando e receber erro.
    // O Ollama roda local e nao usa chave; a OpenAI depende de OPENAI_API_KEY.
    @GetMapping("/status")
    public ResponseEntity<StatusResponse> status() {
        boolean usaOpenAi = "openai".equalsIgnoreCase(provedor);

        // A chave pode vir do ambiente ou ter sido informada na interface. A
        // segunda e o caminho normal: o aplicativo empacotado nao tem variavel
        // de ambiente para configurar.
        boolean chaveOk = configuracaoService.obter().temChaveOpenAi() || chaveDeAmbienteValida();

        boolean ia = !usaOpenAi || chaveOk;
        String mensagem;
        if (!ia) {
            // Sem chave o assistente NAO esta indisponivel: o interpretador
            // proprio atende os comandos escritos. Dizer o contrario esconderia
            // um recurso que funciona.
            mensagem = "Assistente em modo local: os comandos escritos funcionam sem custo. "
                    + "A voz e as frases livres exigem uma chave da OpenAI em Configuracoes.";
        } else if (usaOpenAi) {
            mensagem = "Assistente pronto (OpenAI).";
        } else {
            mensagem = "Assistente pronto (Ollama local). Certifique-se de que o Ollama esta rodando.";
        }
        // A transcricao de voz e sempre da OpenAI: depende da mesma chave.
        return ResponseEntity.ok(new StatusResponse(ia, chaveOk, provedor, mensagem));
    }

    private boolean chaveDeAmbienteValida() {
        return chaveDoAmbiente != null
                && !chaveDoAmbiente.isBlank()
                && !CHAVE_AUSENTE.equals(chaveDoAmbiente);
    }

    @PostMapping(value = "/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ComandoResponse> processarAudio(@RequestParam("arquivo") MultipartFile arquivo) {
        String texto = transcricaoService.transcrever(arquivo);
        String resposta = assistenteService.processarComando(texto);
        return ResponseEntity.ok(new ComandoResponse(texto, resposta));
    }

    @PostMapping("/texto")
    public ResponseEntity<ComandoResponse> processarTexto(@RequestParam("comando") String comando) {
        String resposta = assistenteService.processarComando(comando);
        return ResponseEntity.ok(new ComandoResponse(comando, resposta));
    }
}
