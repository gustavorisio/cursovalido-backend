package com.cursovalido.backend.authentication.validation.login;

import com.cursovalido.backend.authentication.exception.AcessoTemporariamenteBloqueadoException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProtecaoForcaBrutaServiceTest {
    private final Relogio relogio = new Relogio();
    private final ProtecaoForcaBrutaService service = new ProtecaoForcaBrutaService(relogio);
    private static final String EMAIL = "aluno@teste.com";

    @Test
    void bloqueiosDuramQuinzeTrintaESessentaMinutos() {
        for (int minutos : new int[] { 15, 30, 60, 60 }) {
            falharCincoVezes();
            assertThrows(AcessoTemporariamenteBloqueadoException.class, () -> service.verificar(EMAIL));
            relogio.avancar(Duration.ofMinutes(minutos).minusSeconds(1));
            assertThrows(AcessoTemporariamenteBloqueadoException.class, () -> service.verificar(EMAIL));
            relogio.avancar(Duration.ofSeconds(1));
            assertDoesNotThrow(() -> service.verificar(EMAIL));
        }
    }

    @Test
    void pedidosBloqueadosNaoProlongamPrazoENaoAfetamOutrosUsuarios() {
        falharCincoVezes();
        relogio.avancar(Duration.ofMinutes(10));
        service.registrarFalha(" ALUNO@TESTE.COM ");
        assertThrows(AcessoTemporariamenteBloqueadoException.class, () -> service.verificar(" ALUNO@TESTE.COM "));
        assertDoesNotThrow(() -> service.verificar("outro@teste.com"));
        relogio.avancar(Duration.ofMinutes(5));
        assertDoesNotThrow(() -> service.verificar(EMAIL));
    }

    @Test
    void sucessoReiniciaProgressao() {
        falharCincoVezes();
        relogio.avancar(Duration.ofMinutes(15));
        service.registrarSucesso(EMAIL);
        falharCincoVezes();
        relogio.avancar(Duration.ofMinutes(15));
        assertDoesNotThrow(() -> service.verificar(EMAIL));
    }

    @Test
    void quatroFalhasPermitemNovaTentativaEFalhasIsoladasExpiram() {
        for (int i = 0; i < 4; i++)
            service.registrarFalha(EMAIL);
        assertDoesNotThrow(() -> service.verificar(EMAIL));
        relogio.avancar(Duration.ofMinutes(15));
        service.registrarFalha(EMAIL);
        assertDoesNotThrow(() -> service.verificar(EMAIL));
    }

    private void falharCincoVezes() {
        for (int i = 0; i < 5; i++)
            service.registrarFalha(EMAIL);
    }

    private static class Relogio extends Clock {
        private Instant agora = Instant.parse("2026-11-22T12:00:00Z");

        void avancar(Duration duracao) {
            agora = agora.plus(duracao);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }
}
