package com.lucdev.orcamentoia.controller;

import com.lucdev.orcamentoia.config.Autoria;
import com.lucdev.orcamentoia.dto.SobreResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SobreController {

    @GetMapping("/sobre")
    public ResponseEntity<SobreResponse> sobre() {
        return ResponseEntity.ok(new SobreResponse(
                Autoria.PROJETO,
                Autoria.DESCRICAO,
                Autoria.AUTOR,
                Autoria.GITHUB,
                Autoria.LINKEDIN,
                Autoria.INSTAGRAM,
                Autoria.SITE));
    }
}
