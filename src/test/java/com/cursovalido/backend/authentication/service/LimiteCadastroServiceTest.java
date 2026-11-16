package com.cursovalido.backend.authentication.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cursovalido.backend.forum.support.PostgresTeste;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(PostgresTeste.CicloDeVida.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LimiteCadastroServiceTest {
    @Autowired LimiteCadastroService limite;
    @Autowired TransactionTemplate transacoes;

    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry registry) {
        PostgresTeste.configurar(registry, "limite_cadastro");
    }

    @Test
    void terceiroCadastroDoMesmoIpRetorna429() {
        String ip = "192.0.2.1";
        transacoes.executeWithoutResult(status -> limite.reservar(ip));
        transacoes.executeWithoutResult(status -> limite.reservar(ip));

        var falha = assertThrows(ResponseStatusException.class,
                () -> transacoes.executeWithoutResult(status -> limite.reservar(ip)));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, falha.getStatusCode());
        transacoes.executeWithoutResult(status -> limite.reservar("192.0.2.2"));
    }
}
