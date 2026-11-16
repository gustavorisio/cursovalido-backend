package com.cursovalido.backend.authentication.validation.login;

import com.cursovalido.backend.authentication.exception.AcessoTemporariamenteBloqueadoException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProtecaoForcaBrutaServiceTest {

    private final ProtecaoForcaBrutaService service = new ProtecaoForcaBrutaService();

    @Test
    void devePermitirNovaTentativaAntesDoLimite() {
        String email = "aluno@teste.com";
        service.registrarFalha(email);

        assertDoesNotThrow(() -> service.verificar(email));
    }

    @Test
    void deveLimparBloqueioAoRegistrarSucesso() {
        String email = "aluno@teste.com";
        for (int tentativa = 0; tentativa < 5; tentativa++) {
            service.registrarFalha(email);
        }
        assertThrows(AcessoTemporariamenteBloqueadoException.class, () -> service.verificar(email));

        service.registrarSucesso(email);

        assertDoesNotThrow(() -> service.verificar(email));
    }
}

