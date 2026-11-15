package com.cursovalido.backend.authentication.suporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cursovalido.backend.forum.configuration.FlywayConfig;
import com.cursovalido.backend.forum.support.PostgresTeste;
import java.util.HashMap;
import java.util.function.Supplier;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@ExtendWith(PostgresTeste.CicloDeVida.class)
class AtualizacaoBancoLegadoPostgresTest {
    @Test
    void atualizaV2ComChecksumsDaProducaoSemPerderDados() {
        var fonte = fonte("atualizacao_legada");
        var legado = FlywayConfig.configuracao().dataSource(fonte).target("2").load();
        legado.migrate();
        var historico = legado.info().applied();
        assertThat(historico).hasSize(2);
        assertThat(historico[0].getChecksum()).isEqualTo(-1935617500);
        assertThat(historico[1].getChecksum()).isEqualTo(-175556913);

        var jdbc = new JdbcTemplate(fonte);
        jdbc.update("""
                INSERT INTO tb_usuarios
                    (id, nome_completo, email, senha, matricula, declarou_maior_idade, aceitou_termos_lgpd, perfil)
                VALUES (101, 'Conta existente', 'existente@example.com', 'hash', 'LEGADO', true, true, 'ADMINISTRADOR')
                """);
        jdbc.update("UPDATE tb_politicas SET conteudo = 'Documento existente', versao = 'legada'");
        jdbc.update("UPDATE tb_politicas_privacidade SET conteudo = 'Privacidade existente', versao = 'legada'");
        jdbc.update("""
                INSERT INTO tb_contas_excluidas (id, email_hash, bloqueada_ate)
                VALUES (202, 'hash-legado', '2030-01-01T00:00:00Z')
                """);
        jdbc.update("""
                INSERT INTO tb_convites_acesso (id, email, perfil, token_hash, criado_em, expira_em, status)
                VALUES (303, 'convite@example.com', 'PROFESSOR', 'token-legado', CURRENT_TIMESTAMP,
                        CURRENT_TIMESTAMP + interval '1 day', 'PENDENTE')
                """);

        var atual = new FlywayConfig().flyway(fonte, false, "1");
        assertThat(atual.info().pending()).hasSize(1);
        assertThat(atual.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(atual.info().current().getVersion().toString()).isEqualTo("3");
        assertThat(atual.info().applied()[0].getChecksum()).isEqualTo(-1935617500);
        assertThat(atual.info().applied()[1].getChecksum()).isEqualTo(-175556913);
        assertThat(jdbc.queryForObject("SELECT email FROM tb_usuarios WHERE id = 101", String.class))
                .isEqualTo("existente@example.com");
        assertThat(jdbc.queryForObject("SELECT conteudo FROM tb_politicas WHERE versao = 'legada'", String.class))
                .isEqualTo("Documento existente");
        assertThat(jdbc.queryForObject("SELECT conteudo FROM tb_politicas_privacidade WHERE versao = 'legada'", String.class))
                .isEqualTo("Privacidade existente");
        assertThat(jdbc.queryForObject("SELECT bloqueada_ate IS NOT NULL FROM tb_contas_excluidas WHERE id = 202", Boolean.class))
                .isTrue();
        assertThat(jdbc.queryForObject("SELECT token_hash FROM tb_convites_acesso WHERE id = 303", String.class))
                .isEqualTo("token-legado");

        // Exercita as alteracoes exigidas pelo codigo atual sem apagar registros legados.
        jdbc.update("INSERT INTO tb_contas_excluidas (id, email_hash) VALUES (203, 'hash-novo')");
        jdbc.update("UPDATE tb_usuarios SET perfil = 'ALUNO', excluido_em = CURRENT_TIMESTAMP WHERE id = 101");
        jdbc.update("""
                INSERT INTO tb_convites_acesso (email, perfil, token_hash, criado_em, expira_em, status, criado_por)
                VALUES ('convite@example.com', 'PROFESSOR', 'token-novo', CURRENT_TIMESTAMP,
                        CURRENT_TIMESTAMP + interval '1 day', 'PENDENTE', 101)
                """);
        jdbc.update("""
                INSERT INTO tb_aceites_responsabilidade (user_id, perfil, termo_version, ip_address, accepted_at)
                VALUES (101, 'PROFESSOR', '1.0', '127.0.0.1', CURRENT_TIMESTAMP)
                """);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tb_convites_acesso WHERE email = 'convite@example.com'", Long.class))
                .isEqualTo(2);
        assertThat(atual.migrate().migrationsExecuted).isZero();
    }

    @Test
    void checksumAlteradoContinuaImpedindoAtualizacao() {
        var fonte = fonte("checksum_legado");
        FlywayConfig.configuracao().dataSource(fonte).target("2").load().migrate();
        var jdbc = new JdbcTemplate(fonte);
        jdbc.update("UPDATE flyway_schema_history SET checksum = checksum + 1 WHERE version = '1'");

        assertThatThrownBy(() -> FlywayConfig.configuracao().dataSource(fonte).load().migrate())
                .isInstanceOf(FlywayValidateException.class)
                .hasMessageContaining("checksum mismatch");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version = '3'", Long.class))
                .isZero();
    }

    private DriverManagerDataSource fonte(String schema) {
        var propriedades = new HashMap<String, Supplier<?>>();
        PostgresTeste.configurar(propriedades::put, schema);
        return new DriverManagerDataSource(
                propriedades.get("spring.datasource.url").get().toString(),
                propriedades.get("spring.datasource.username").get().toString(),
                propriedades.get("spring.datasource.password").get().toString());
    }
}
