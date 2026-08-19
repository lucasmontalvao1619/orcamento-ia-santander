package com.lucdev.orcamentoia.controller;

import com.lucdev.orcamentoia.dto.ComandoResponse;
import com.lucdev.orcamentoia.service.AssistenteService;
import com.lucdev.orcamentoia.service.TranscricaoService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/assistente")
public class AssistenteController {

    private final TranscricaoService transcricaoService;
    private final AssistenteService assistenteService;

    public AssistenteController(TranscricaoService transcricaoService, AssistenteService assistenteService) {
        this.transcricaoService = transcricaoService;
        this.assistenteService = assistenteService;
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
