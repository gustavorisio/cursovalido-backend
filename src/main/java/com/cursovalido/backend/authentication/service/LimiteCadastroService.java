package com.cursovalido.backend.authentication.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class LimiteCadastroService {
    private static final int MAXIMO = 2;
    private final JdbcTemplate jdbc;

    @Transactional(propagation = Propagation.MANDATORY)
    public void reservar(String enderecoIp) {
        if (enderecoIp == null || enderecoIp.isBlank()) return;
        for (int vaga = 1; vaga <= MAXIMO; vaga++) {
            if (jdbc.update("INSERT INTO tb_cadastro_vagas (chave) VALUES (?) ON CONFLICT DO NOTHING",
                    enderecoIp + ":" + vaga) == 1) return;
        }
        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Limite de dois cadastros por endereço IP atingido. Se precisar de ajuda, procure o Suporte.");
    }
}
