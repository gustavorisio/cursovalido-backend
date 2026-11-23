package com.cursovalido.backend.authentication.service;

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
class DocumentoVersoesMigracaoTest {
    @Test
    void bancoNovoFicaSemDocumentosLegaisAtePublicacaoPeloRoot() {
        var jdbc = migrar("documentos_unificados");

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tb_politicas", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tb_politicas_privacidade", Long.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT numero FROM tb_versoes_documentos WHERE tipo = 'TERMOS'", Long.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT numero FROM tb_versoes_documentos WHERE tipo = 'PRIVACIDADE'", Long.class)).isZero();
    }

    private JdbcTemplate migrar(String schema) {
        var propriedades = new HashMap<String, Supplier<?>>();
        PostgresTeste.configurar(propriedades::put, schema);
        var fonte = new DriverManagerDataSource(
                propriedades.get("spring.datasource.url").get().toString(),
                propriedades.get("spring.datasource.username").get().toString(),
                propriedades.get("spring.datasource.password").get().toString());
        Flyway flyway = FlywayConfig.configuracao().dataSource(fonte).load();

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(5);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("5");
        return new JdbcTemplate(fonte);
    }
}
