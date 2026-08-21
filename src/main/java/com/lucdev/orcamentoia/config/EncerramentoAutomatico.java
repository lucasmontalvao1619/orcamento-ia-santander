package com.lucdev.orcamentoia.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

// Rodando como aplicativo de duplo clique, o usuario espera que fechar a janela
// feche o programa. So que a interface e uma pagina web: fechar a aba nao
// derruba o servidor, e a aplicacao ficaria rodando invisivel para sempre.
//
// A pagina manda um sinal de vida periodico enquanto esta aberta. Quando os
// sinais param, ninguem esta mais usando e a aplicacao se encerra.
//
// Vale so no modo aplicativo. Em container ou servidor, encerrar porque nao ha
// navegador aberto seria exatamente o comportamento errado.
@Component
public class EncerramentoAutomatico {

    private static final Logger log = LoggerFactory.getLogger(EncerramentoAutomatico.class);

    // Precisa cobrir um F5: entre o unload e o load da pagina nova os sinais
    // param por um instante, e encerrar ali seria fechar o app a cada refresh.
    static final Duration TOLERANCIA = Duration.ofSeconds(15);

    // Se a janela nunca abrir (navegador bloqueado, usuario fechou antes de
    // carregar), a aplicacao nao pode ficar de pe indefinidamente.
    static final Duration ESPERA_INICIAL = Duration.ofMinutes(3);

    private final boolean habilitado;
    private final ApplicationContext contexto;
    private final long inicio = System.currentTimeMillis();

    private volatile long ultimoSinal = 0;

    public EncerramentoAutomatico(
            @Value("${app.encerrar-ao-fechar:false}") boolean habilitado,
            ApplicationContext contexto) {
        this.habilitado = habilitado;
        this.contexto = contexto;
    }

    public void registrarSinalDeVida() {
        ultimoSinal = System.currentTimeMillis();
    }

    public boolean isHabilitado() {
        return habilitado;
    }

    @Scheduled(fixedDelay = 3000, initialDelay = 5000)
    public void verificar() {
        if (!habilitado) {
            return;
        }
        if (!deveEncerrar(System.currentTimeMillis(), ultimoSinal, inicio)) {
            return;
        }
        log.info("Janela fechada: encerrando a aplicacao.");
        // Em thread propria porque o encerramento espera as tarefas agendadas
        // terminarem — inclusive esta, que ficaria esperando a si mesma.
        new Thread(() -> System.exit(SpringApplication.exit(contexto, () -> 0)), "encerramento").start();
    }

    // Separado da tarefa agendada para o teste conseguir exercitar a decisao
    // sem esperar em relogio real nem derrubar a JVM do teste.
    static boolean deveEncerrar(long agora, long ultimoSinal, long inicio) {
        if (ultimoSinal == 0) {
            // Nenhuma janela jamais se apresentou.
            return agora - inicio > ESPERA_INICIAL.toMillis();
        }
        return agora - ultimoSinal > TOLERANCIA.toMillis();
    }
}
