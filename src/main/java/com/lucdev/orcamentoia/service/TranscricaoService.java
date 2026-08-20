package com.lucdev.orcamentoia.service;

import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class TranscricaoService {

    private final OpenAiAudioTranscriptionModel transcriptionModel;

    public TranscricaoService(OpenAiAudioTranscriptionModel transcriptionModel) {
        this.transcriptionModel = transcriptionModel;
    }

    public String transcrever(MultipartFile audio) {
        Resource recurso = toResource(audio);

        OpenAiAudioTranscriptionOptions opcoes = OpenAiAudioTranscriptionOptions.builder()
                .language("pt")
                .responseFormat(OpenAiAudioApi.TranscriptResponseFormat.TEXT)
                .temperature(0f)
                .build();

        AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(recurso, opcoes);
        return transcriptionModel.call(prompt).getResult().getOutput();
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
