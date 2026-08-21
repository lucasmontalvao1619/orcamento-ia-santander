package com.lucdev.orcamentoia.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.audio.transcription.AudioTranscription;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranscricaoServiceTest {

    @Mock
    private OpenAiAudioTranscriptionModel transcriptionModel;

    @InjectMocks
    private TranscricaoService transcricaoService;

    @Test
    void devolveOTextoTranscrito() {
        when(transcriptionModel.call(any(AudioTranscriptionPrompt.class)))
                .thenReturn(new AudioTranscriptionResponse(new AudioTranscription("gastei 60 no uber")));

        String texto = transcricaoService.transcrever(
                new MockMultipartFile("arquivo", "comando.webm", "audio/webm", "audio".getBytes()));

        assertThat(texto).isEqualTo("gastei 60 no uber");
    }

    // A OpenAI identifica o formato do audio pela extensao do nome do arquivo.
    // Um ByteArrayResource comum devolve filename nulo, e a chamada e recusada;
    // por isso o service sobrescreve getFilename().
    @Test
    void preservaONomeDoArquivoParaOProvedorReconhecerOFormato() {
        when(transcriptionModel.call(any(AudioTranscriptionPrompt.class)))
                .thenReturn(new AudioTranscriptionResponse(new AudioTranscription("ok")));

        transcricaoService.transcrever(
                new MockMultipartFile("arquivo", "comando.webm", "audio/webm", "audio".getBytes()));

        ArgumentCaptor<AudioTranscriptionPrompt> prompt = ArgumentCaptor.forClass(AudioTranscriptionPrompt.class);
        verify(transcriptionModel).call(prompt.capture());
        assertThat(prompt.getValue().getInstructions().getFilename()).isEqualTo("comando.webm");
    }

    // Arquivo ilegivel e erro de entrada, nao falha do provedor: vira
    // IllegalArgumentException, que o handler traduz para 400 em vez de 500.
    @Test
    void arquivoIlegivelViraErroDeEntrada() throws IOException {
        MultipartFile quebrado = mock(MultipartFile.class);
        when(quebrado.getBytes()).thenThrow(new IOException("falha de leitura"));

        assertThatThrownBy(() -> transcricaoService.transcrever(quebrado))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("audio");

        verify(transcriptionModel, never()).call(any(AudioTranscriptionPrompt.class));
    }
}
