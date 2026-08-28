package com.lucdev.orcamentoia.service;

import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class TranscricaoService {

    private final OpenAiAudioTranscriptionModel modeloDaInicializacao;
    private final ConfiguracaoService configuracaoService;

    public TranscricaoService(OpenAiAudioTranscriptionModel modeloDaInicializacao,
                              ConfiguracaoService configuracaoService) {
        this.modeloDaInicializacao = modeloDaInicializacao;
        this.configuracaoService = configuracaoService;
    }

    // A chave pode ter sido informada pela interface depois que a aplicacao
    // subiu. O bean criado na inicializacao carrega a chave do ambiente e nao
    // muda mais; por isso, havendo chave configurada, o cliente e montado aqui,
    // na hora da chamada. Sem isto, salvar a chave exigiria reiniciar o app.
    private OpenAiAudioTranscriptionModel modelo() {
        String chave = configuracaoService.obter().getChaveOpenAi();
        if (chave == null || chave.isBlank()) {
            return modeloDaInicializacao;
        }
        return new OpenAiAudioTranscriptionModel(
                OpenAiAudioApi.builder().apiKey(chave).build());
    }

    public String transcrever(MultipartFile audio) {
        Resource recurso = toResource(audio);

        OpenAiAudioTranscriptionOptions opcoes = OpenAiAudioTranscriptionOptions.builder()
                .language("pt")
                .responseFormat(OpenAiAudioApi.TranscriptResponseFormat.TEXT)
                .temperature(0f)
                .build();

        AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(recurso, opcoes);
        return modelo().call(prompt).getResult().getOutput();
    }

    private Resource toResource(MultipartFile audio) {
        try {
            return new ByteArrayResource(audio.getBytes()) {
                @Override
                public String getFilename() {
                    return audio.getOriginalFilename();
                }
            };
        } catch (IOException e) {
            throw new IllegalArgumentException("Nao foi possivel ler o arquivo de audio enviado.", e);
        }
    }
}
