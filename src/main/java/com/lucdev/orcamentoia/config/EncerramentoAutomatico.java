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

    // Tolerancia longa de proposito. Navegadores congelam temporizadores de
    // abas em segundo plano — o Safari e agressivo nisso —, entao os sinais
    // param sem que a janela tenha sido fechada. Com 15 segundos, trocar de aba
    // por meio minuto matava a aplicacao com o usuario ainda usando.
    //
    // O fechamento rapido nao depende disto: a pagina avisa ao ser fechada
    // (ver AVISO_DE_FECHAMENTO). Este prazo e so a rede de seguranca para
    // quando o aviso nao chega — navegador encerrado a forca, queda de energia.
    static final Duration TOLERANCIA = Duration.ofMinutes(10);

    // Depois do aviso de fechamento, esta e a espera antes de encerrar. Existe
    // porque recarregar a pagina (F5) dispara o mesmo aviso: se for recarga, o
    // proximo sinal chega em segundos e cancela o encerramento.
    static final Duration ESPERA_APOS_AVISO = Duration.ofSeconds(8);

    // Se a janela nunca abrir (navegador bloqueado, usuario fechou antes de
    // carregar), a aplicacao nao pode ficar de pe indefinidamente.
    static final Duration ESPERA_INICIAL = Duration.ofMinutes(3);

    private final boolean habilitado;
    private final ApplicationContext contexto;
    private final long inicio = System.currentTimeMillis();

    private volatile long ultimoSinal = 0;

    // Quando a pagina avisou que estava fechando. Zero significa que nao houve
    // aviso pendente.
    private volatile long avisoDeFechamento = 0;

    public EncerramentoAutomatico(
            @Value("${app.encerrar-ao-fechar:false}") boolean habilitado,
            ApplicationContext contexto) {
        this.habilitado = habilitado;
        this.contexto = contexto;
    }

    public void registrarSinalDeVida() {
        ultimoSinal = System.currentTimeMillis();
        // Um sinal cancela qualquer aviso de fechamento pendente: era recarga
        // da pagina, e nao fechamento.
        avisoDeFechamento = 0;
    }

    // A pagina avisa ao ser fechada. Nao encerra na hora: recarregar dispara o
    // mesmo evento, e fechar o app a cada F5 seria pior que demorar 8 segundos.
    public void registrarAvisoDeFechamento() {
        avisoDeFechamento = System.currentTimeMillis();
    }

    public boolean isHabilitado() {
        return habilitado;
    }

    @Scheduled(fixedDelay = 3000, initialDelay = 5000)
    public void verificar() {
        if (!habilitado) {
            return;
        }
        long agora = System.currentTimeMillis();
        if (!deveEncerrar(agora, ultimoSinal, inicio)
                && !avisoDeFechamentoExpirou(agora, avisoDeFechamento)) {
            return;
        }
        log.info("Janela fechada: encerrando a aplicacao.");
        // Em thread propria porque o encerramento espera as tarefas agendadas
        // terminarem — inclusive esta, que ficaria esperando a si mesma.
        new Thread(() -> System.exit(SpringApplication.exit(contexto, () -> 0)), "encerramento").start();
    }

    // Separado da tarefa agendada para o teste conseguir exercitar a decisao
    // sem esperar em relogio real nem derrubar a JVM do teste.
    static boolean avisoDeFechamentoExpirou(long agora, long avisoDeFechamento) {
        return avisoDeFechamento != 0 && agora - avisoDeFechamento > ESPERA_APOS_AVISO.toMillis();
    }

    static boolean deveEncerrar(long agora, long ultimoSinal, long inicio) {
        if (ultimoSinal == 0) {
            // Nenhuma janela se apresentou ate agora — e isso NAO e motivo para
            // encerrar. Uma pagina antiga em cache do navegador nao envia sinal
            // nenhum: o app se matava sozinho enquanto o usuario tentava usa-lo,
            // e todo comando virava "nao foi possivel falar com o servidor".
            //
            // Encerrar so quando os sinais COMECARAM e depois pararam: ai a
            // janela existiu e foi fechada, que e o caso que este recurso
            // pretende cobrir.
            return false;
        }
        return agora - ultimoSinal > TOLERANCIA.toMillis();
    }
}
