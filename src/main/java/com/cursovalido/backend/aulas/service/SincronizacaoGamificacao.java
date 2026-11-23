package com.cursovalido.backend.aulas.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class SincronizacaoGamificacao {
    private final GamificacaoService gamificacao;

    // Atualiza apenas as métricas de cursos concluídos. Não concede XP.
    @EventListener(ApplicationReadyEvent.class)
    public void sincronizarConclusoesAnteriores() {
        int registradas = 0;
        try {
            for (var matricula : gamificacao.matriculasPendentesDeConclusao()) {
                try {
                    if (gamificacao.registrarConclusao(matricula.getIdAluno(), matricula.getIdCurso(),
                            matricula.getId()))
                        registradas++;
                } catch (RuntimeException excecao) {
                    log.warn("Nao foi possivel atualizar a conclusao da matricula {}", matricula.getId());
                }
            }
        } catch (RuntimeException excecao) {
            log.warn("Sincronizacao de conclusoes de cursos indisponivel no momento");
            return;
        }
        if (registradas > 0)
            log.info("{} conclusoes de cursos registradas na gamificacao", registradas);
    }
}
