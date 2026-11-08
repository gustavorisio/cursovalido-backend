package com.cursovalido.backend.authentication.suporte;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class LimiteSuporteService {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transacao;
    private final Clock relogio;

    @Autowired
    public LimiteSuporteService(JdbcTemplate jdbc, PlatformTransactionManager gerenciador) {
        this(jdbc, gerenciador, Clock.systemUTC());
    }

    LimiteSuporteService(JdbcTemplate jdbc, PlatformTransactionManager gerenciador, Clock relogio) {
        this.jdbc = jdbc;
        this.transacao = new TransactionTemplate(gerenciador);
        this.relogio = relogio;
    }

    public void enviar(String ip, String emailConta, String emailContato, Runnable envio) {
        var chaves = Stream.of("ip:" + ip, "email:" + normalizar(emailConta), "email:" + normalizar(emailContato))
                .map(LimiteSuporteService::hash).distinct().toList();
        transacao.executeWithoutResult(status -> {
            // Trava compartilhada entre instancias: duas requisicoes simultaneas nao ultrapassam o limite.
            jdbc.queryForObject("SELECT id FROM tb_suporte_controle WHERE id = 1 FOR UPDATE", Integer.class);
            Instant agora = relogio.instant();
            Instant liberacao = agora;
            for (String chave : chaves) {
                var expiracoes = jdbc.query("SELECT expira_em FROM tb_suporte_limites WHERE chave = ?",
                        (rs, linha) -> rs.getTimestamp(1).toInstant(), chave);
                for (Instant expiracao : expiracoes) {
                    if (expiracao.isAfter(liberacao)) liberacao = expiracao;
                }
            }
            if (liberacao.isAfter(agora)) {
                throw new LimiteSuporteException(Math.max(1, (Duration.between(agora, liberacao).toMillis() + 999) / 1000));
            }
            envio.run();
            // Falha no provedor desfaz a transacao e nao consome a cota diaria.
            Instant fim = relogio.instant().plus(Duration.ofHours(24));
            jdbc.update("DELETE FROM tb_suporte_limites WHERE expira_em <= ?", Timestamp.from(agora));
            for (String chave : chaves) {
                jdbc.update("INSERT INTO tb_suporte_limites (chave, expira_em) VALUES (?, ?)", chave, Timestamp.from(fim));
            }
        });
    }

    private static String normalizar(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private static String hash(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException excecao) {
            throw new IllegalStateException(excecao);
        }
    }
}
