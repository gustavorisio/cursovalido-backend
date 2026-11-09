package com.cursovalido.backend.authentication.suporte;

import java.time.*;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import static org.assertj.core.api.Assertions.*;

class LimiteSuporteServiceTest {
    private JdbcTemplate jdbc;
    private DataSourceTransactionManager transacoes;
    private final Instant agora = Instant.parse("2026-11-08T00:00:00Z");
    private LimiteSuporteService servico;

    @BeforeEach
    void preparar() {
        var fonte = new JdbcDataSource();
        fonte.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000");
        jdbc = new JdbcTemplate(fonte);
        transacoes = new DataSourceTransactionManager(fonte);
        jdbc.execute("CREATE TABLE tb_suporte_controle (id INTEGER PRIMARY KEY)");
        jdbc.update("INSERT INTO tb_suporte_controle VALUES (1)");
        jdbc.execute(
                "CREATE TABLE tb_suporte_limites (chave VARCHAR(80) PRIMARY KEY, expira_em TIMESTAMP WITH TIME ZONE NOT NULL)");
        servico = novo(agora);
    }

    private LimiteSuporteService novo(Instant instante) {
        return new LimiteSuporteService(jdbc, transacoes, Clock.fixed(instante, ZoneOffset.UTC));
    }

    private void enviar(String ip, String conta, String contato) {
        servico.enviar(ip, conta, contato, () -> {
        });
    }

    @Test
    void bloqueiaMesmoIpMesmoComEmailsDiferentes() {
        enviar("1", "a@example.com", "a@example.com");
        assertThatThrownBy(() -> enviar("1", "b@example.com", "b@example.com"))
                .isInstanceOf(LimiteSuporteException.class);
    }

    @Test
    void bloqueiaContaEContatoEmOutroIpComNormalizacao() {
        enviar("1", "a@example.com", "retorno@example.com");
        assertThatThrownBy(() -> enviar("2", " A@EXAMPLE.COM ", "novo@example.com"))
                .isInstanceOf(LimiteSuporteException.class);
        assertThatThrownBy(() -> enviar("3", "outro@example.com", "RETORNO@example.com"))
                .isInstanceOf(LimiteSuporteException.class);
        enviar("4", "livre@example.com", "livre@example.com");
    }

    @Test
    void mantemLimiteAoRecriarServicoELiberaApos24Horas() {
        enviar("1", "a@example.com", "a@example.com");
        assertThatThrownBy(() -> novo(agora.plusSeconds(86399)).enviar("1", "a@example.com", "a@example.com", () -> {
        }))
                .isInstanceOfSatisfying(LimiteSuporteException.class,
                        erro -> assertThat(erro.getHeaders().getFirst("Retry-After")).isEqualTo("1"));
        novo(agora.plusSeconds(86400)).enviar("1", "a@example.com", "a@example.com", () -> {
        });
    }

    @Test
    void falhaDeEmailNaoConsomeCota() {
        assertThatThrownBy(() -> servico.enviar("1", "a@example.com", "a@example.com",
                () -> {
                    throw new IllegalStateException("falha externa");
                })).isInstanceOf(IllegalStateException.class);
        enviar("1", "a@example.com", "a@example.com");
    }

    @Test
    void apenasUmaRequisicaoSimultaneaEnviaEmail() throws Exception {
        var inicio = new CountDownLatch(1);
        var envios = new AtomicInteger();
        Callable<Boolean> tarefa = () -> {
            inicio.await();
            try {
                novo(agora).enviar("1", "a@example.com", "a@example.com", envios::incrementAndGet);
                return true;
            } catch (LimiteSuporteException esperado) {
                return false;
            }
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var primeiro = executor.submit(tarefa);
            var segundo = executor.submit(tarefa);
            inicio.countDown();
            assertThat(primeiro.get(10, TimeUnit.SECONDS) ^ segundo.get(10, TimeUnit.SECONDS)).isTrue();
            assertThat(envios.get()).isEqualTo(1);
        }
    }
}
