package com.cursovalido.backend.authentication.validation.login;

import com.cursovalido.backend.authentication.exception.AcessoTemporariamenteBloqueadoException;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class ProtecaoForcaBrutaService {
    private static final int LIMITE_TENTATIVAS = 5;
    private final Map<String, Tentativa> tentativas = new ConcurrentHashMap<>();
    private final Clock relogio;

    public ProtecaoForcaBrutaService() {
        this(Clock.systemUTC());
    }

    ProtecaoForcaBrutaService(Clock relogio) {
        this.relogio = relogio;
    }

    public void verificar(String identificador) {
        tentativas.computeIfPresent(chave(identificador), (chave, tentativa) -> {
            if (tentativa.bloqueadoAte != null && relogio.instant().isBefore(tentativa.bloqueadoAte))
                throw new AcessoTemporariamenteBloqueadoException();
            expirarFalhas(tentativa, relogio.instant());
            return tentativa;
        });
    }

    public void registrarFalha(String identificador) {
        tentativas.compute(chave(identificador), (chave, tentativa) -> {
            Instant agora = relogio.instant();
            if (tentativa == null) tentativa = new Tentativa();
            // Pedidos durante um bloqueio não o prolongam nem avancam o nivel.
            if (tentativa.bloqueadoAte != null && agora.isBefore(tentativa.bloqueadoAte))
                return tentativa;
            expirarFalhas(tentativa, agora);
            tentativa.ultimaFalha = agora;
            if (++tentativa.falhas >= LIMITE_TENTATIVAS) {
                tentativa.nivel = Math.min(3, tentativa.nivel + 1);
                int minutos = switch (tentativa.nivel) {
                    case 1 -> 15;
                    case 2 -> 30;
                    default -> 60;
                };
                tentativa.bloqueadoAte = agora.plusSeconds(minutos * 60L);
                tentativa.falhas = 0;
            }
            return tentativa;
        });
    }

    // Somente a autenticação concluida reinicia a progressao dos bloqueios.
    public void registrarSucesso(String identificador) {
        tentativas.remove(chave(identificador));
    }

    private static String chave(String identificador) {
        return identificador.trim().toLowerCase(Locale.ROOT);
    }

    private static void expirarFalhas(Tentativa tentativa, Instant agora) {
        if (tentativa.bloqueadoAte != null && !agora.isBefore(tentativa.bloqueadoAte)) {
            tentativa.bloqueadoAte = null;
            tentativa.falhas = 0;
        } else if (tentativa.bloqueadoAte == null && tentativa.ultimaFalha != null
                && !agora.isBefore(tentativa.ultimaFalha.plusSeconds(15 * 60L))) {
            tentativa.falhas = 0;
        }
    }

    private static class Tentativa {
        private int falhas;
        private int nivel;
        private Instant bloqueadoAte;
        private Instant ultimaFalha;
    }
}
