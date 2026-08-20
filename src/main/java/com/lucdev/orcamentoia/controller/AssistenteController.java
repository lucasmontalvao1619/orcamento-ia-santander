package com.lucdev.orcamentoia.controller;

import com.lucdev.orcamentoia.dto.ComandoResponse;
import com.lucdev.orcamentoia.dto.StatusResponse;
import com.lucdev.orcamentoia.service.AssistenteService;
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
    private final String provedor;
    private final String chaveOpenAi;

    public AssistenteController(TranscricaoService transcricaoService,
                                AssistenteService assistenteService,
                                @Value("${spring.ai.model.chat:ollama}") String provedor,
                                @Value("${spring.ai.openai.api-key:}") String chaveOpenAi) {
        this.transcricaoService = transcricaoService;
        this.assistenteService = assistenteService;
        this.provedor = provedor;
        this.chaveOpenAi = chaveOpenAi;
    }

    // A interface chama este endpoint ao abrir. Sem ele o usuario so descobriria
    // que falta configuracao depois de mandar um comando e receber erro.
    // O Ollama roda local e nao usa chave; a OpenAI depende de OPENAI_API_KEY.
    @GetMapping("/status")
    public ResponseEntity<StatusResponse> status() {
        boolean usaOpenAi = "openai".equalsIgnoreCase(provedor);
        boolean chaveOk = chaveOpenAi != null
                && !chaveOpenAi.isBlank()
                && !CHAVE_AUSENTE.equals(chaveOpenAi);

        boolean ia = usaOpenAi ? chaveOk : true;
        String mensagem;
        if (!ia) {
            mensagem = "Defina a variavel de ambiente OPENAI_API_KEY para habilitar o assistente.";
        } else if (usaOpenAi) {
            mensagem = "Assistente pronto (OpenAI).";
        } else {
            mensagem = "Assistente pronto (Ollama local). Certifique-se de que o Ollama esta rodando.";
        }
        // A transcricao no servidor e sempre da OpenAI, independente do provedor
        // de chat, porque o Ollama nao transcreve audio.
        return ResponseEntity.ok(new StatusResponse(ia, chaveOk, provedor, mensagem));
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
