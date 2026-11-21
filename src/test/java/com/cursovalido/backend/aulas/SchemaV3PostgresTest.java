package com.cursovalido.backend.aulas;

import static org.assertj.core.api.Assertions.assertThat;

import com.cursovalido.backend.forum.support.PostgresTeste;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@ExtendWith(PostgresTeste.CicloDeVida.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SchemaV3PostgresTest {
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    Flyway flyway;

    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry registry) {
        PostgresTeste.configurar(registry, "schema_unificado");
    }

    @Test
    void bancoNovoInicializaComUmaUnicaV21EDeixaDocumentosParaOPublicacaoDoRoot() {
        assertThat(flyway.info().applied()).extracting(item -> item.getVersion().getVersion()).containsExactly("21");
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tb_politicas", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tb_politicas_privacidade", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT to_regclass('tb_gamificacao_alunos') IS NOT NULL", Boolean.class))
                .isTrue();
        assertThat(jdbc.queryForObject("SELECT to_regclass('tb_denuncias_forum') IS NOT NULL", Boolean.class))
                .isTrue();
    }
}
