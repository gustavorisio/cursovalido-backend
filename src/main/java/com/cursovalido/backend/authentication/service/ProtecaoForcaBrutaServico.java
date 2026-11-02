package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.exception.AcessoTemporariamenteBloqueadoException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class ProtecaoForcaBrutaServico {
    private static final int LIMITE_TENTATIVAS = 5;
    private static final int ATRASO_MAXIMO_SEGUNDOS = 2;
    private static final int MINUTOS_BLOQUEIO = 15;

    private final Map<String, Tentativa> tentativas = new ConcurrentHashMap<>();

    // Verifica se o acesso pode continuar e aplica o atraso necessario.
    public void verificar(String identificador) {
        Tentativa tentativa = tentativas.get(identificador);
        if (tentativa == null)
            return;

        if (tentativa.bloqueadoAte != null) {
            if (tentativa.bloqueadoAte.isAfter(LocalDateTime.now()))
                throw new AcessoTemporariamenteBloqueadoException();
            tentativas.remove(identificador);
            return;
        }

        atrasar(tentativa.falhas);
    }

    // Registra uma falha e bloqueia apos muitas tentativas.
    public void registrarFalha(String identificador) {
        tentativas.compute(identificador, (chave, tentativa) -> {
            if (tentativa == null)
                tentativa = new Tentativa();
            tentativa.falhas++;
            if (tentativa.falhas >= LIMITE_TENTATIVAS)
                tentativa.bloqueadoAte = LocalDateTime.now().plusMinutes(MINUTOS_BLOQUEIO);
            return tentativa;
        });
    }

    // Limpa as falhas depois de um acesso correto.
    public void registrarSucesso(String identificador) {
        tentativas.remove(identificador);
    }

    private void atrasar(int falhas) {
        long segundos = Math.min(falhas, ATRASO_MAXIMO_SEGUNDOS);
        try {
            Thread.sleep(segundos * 1000L);
        } catch (InterruptedException excecao) {
            Thread.currentThread().interrupt();
        }
    }

    private static class Tentativa {
        private int falhas;
        private LocalDateTime bloqueadoAte;
    }
}