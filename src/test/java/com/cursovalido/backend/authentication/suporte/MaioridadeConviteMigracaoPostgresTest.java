package com.cursovalido.backend.authentication.suporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cursovalido.backend.forum.configuration.FlywayConfig;
import com.cursovalido.backend.forum.support.PostgresTeste;
import java.util.HashMap;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@ExtendWith(PostgresTeste.CicloDeVida.class)
class MaioridadeConviteMigracaoPostgresTest {
    @Test
    void schemaUnificadoMantemRegrasAtuaisDeContaEConvite() {
        var propriedades = new HashMap<String, Supplier<?>>();
        PostgresTeste.configurar(propriedades::put, "convites_unificados");
        var fonte = new DriverManagerDataSource(
                propriedades.get("spring.datasource.url").get().toString(),
                propriedades.get("spring.datasource.username").get().toString(),
                propriedades.get("spring.datasource.password").get().toString());
        var flyway = FlywayConfig.configuracao().dataSource(fonte).load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        var jdbc = new JdbcTemplate(fonte);

        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = current_schema() AND table_name = 'tb_usuarios'
                  AND column_name = 'data_nascimento'
                """, Integer.class)).isZero();
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = current_schema() AND table_name = 'tb_convites_acesso'
                  AND column_name IN ('enviado_em', 'quantidade_envios')
                """, Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM pg_indexes
                WHERE schemaname = current_schema() AND indexname = 'uk_convites_pendente_por_email'
                """, Integer.class)).isEqualTo(1);

        jdbc.update("""
                INSERT INTO tb_convites_acesso (email, perfil, token_hash, criado_em, expira_em, status)
                VALUES ('prof@example.com', 'PROFESSOR', 'token-1', now(), now() + interval '1 day', 'PENDENTE')
                """);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO tb_convites_acesso (email, perfil, token_hash, criado_em, expira_em, status)
                VALUES ('prof@example.com', 'PROFESSOR', 'token-2', now(), now() + interval '1 day', 'PENDENTE')
                """)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
