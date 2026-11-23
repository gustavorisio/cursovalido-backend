package com.cursovalido.backend.authentication.suporte;

import static org.assertj.core.api.Assertions.assertThat;

import com.cursovalido.backend.forum.configuration.FlywayConfig;
import com.cursovalido.backend.forum.support.PostgresTeste;
import java.util.HashMap;
import java.util.function.Supplier;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@ExtendWith(PostgresTeste.CicloDeVida.class)
class AtualizacaoBancoLegadoPostgresTest {
    @Test
    void bancoVazioRecebeSchemaInicialEAtualizacoes() {
        var propriedades = new HashMap<String, Supplier<?>>();
        PostgresTeste.configurar(propriedades::put, "schema_unificado");
        var fonte = new DriverManagerDataSource(
                propriedades.get("spring.datasource.url").get().toString(),
                propriedades.get("spring.datasource.username").get().toString(),
                propriedades.get("spring.datasource.password").get().toString());
        Flyway flyway = FlywayConfig.configuracao().dataSource(fonte).load();
        flyway.migrate();

        assertThat(flyway.info().applied()).extracting(item -> item.getVersion().getVersion()).containsExactly("1", "2", "3", "4", "5", "6");
        assertThat(flyway.info().pending()).isEmpty();

        var jdbc = new JdbcTemplate(fonte);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success", Long.class))
                .isEqualTo(6);
        assertThat(jdbc.queryForObject("SELECT to_regclass('tb_usuarios') IS NOT NULL", Boolean.class)).isTrue();
        assertThat(jdbc.queryForObject("SELECT to_regclass('tb_cursos') IS NOT NULL", Boolean.class)).isTrue();
        assertThat(jdbc.queryForObject("SELECT to_regclass('tb_denuncias_forum') IS NOT NULL", Boolean.class)).isTrue();
    }
}
