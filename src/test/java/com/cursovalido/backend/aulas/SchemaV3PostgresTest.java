package com.cursovalido.backend.aulas;

import static org.assertj.core.api.Assertions.assertThat;

import com.cursovalido.backend.forum.configuration.FlywayConfig;
import com.cursovalido.backend.forum.support.PostgresTeste;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@ExtendWith(PostgresTeste.CicloDeVida.class)
@Import(SchemaV3PostgresTest.BancoLimpo.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SchemaV3PostgresTest extends AulasIntegracaoTest {
        @Autowired
        DataSource fonte;
        @Autowired
        JdbcTemplate jdbc;
        @Autowired
        Flyway flyway;

        @DynamicPropertySource
        static void banco(DynamicPropertyRegistry registry) {
                PostgresTeste.configurar(registry, "schema_v_tres");
        }

        @Test
        void execucaoManualComBaselineEquivaleAInicializacaoAutomatica() {
                String referencia = "referencia_" + java.util.UUID.randomUUID().toString().replace("-", "");
                var flywayReferencia = FlywayConfig.configuracao().dataSource(fonte).schemas(referencia)
                                .defaultSchema(referencia).load();
                flywayReferencia.migrate();
                String colunas = """
                                select table_name, column_name, data_type, is_nullable, character_maximum_length
                                from information_schema.columns
                                where table_schema = ? and table_name <> 'flyway_schema_history'
                                order by table_name, column_name
                                """;
                assertThat(jdbc.queryForList(colunas, "schema_v_tres"))
                                .containsExactlyElementsOf(jdbc.queryForList(colunas, referencia));
                String restricoes = """
                                select t.relname, c.contype, replace(pg_get_constraintdef(c.oid), ? || '.', '') as definicao
                                from pg_constraint c join pg_class t on t.oid = c.conrelid
                                join pg_namespace n on n.oid = t.relnamespace
                                where n.nspname = ? and t.relname <> 'flyway_schema_history'
                                order by t.relname, c.contype, definicao
                                """;
                assertThat(jdbc.queryForList(restricoes, "schema_v_tres", "schema_v_tres"))
                                .containsExactlyElementsOf(jdbc.queryForList(restricoes, referencia, referencia));
                String indices = """
                                select tablename, indexname, replace(indexdef, ? || '.', '') as definicao
                                from pg_indexes where schemaname = ? and tablename <> 'flyway_schema_history'
                                order by tablename, indexname
                                """;
                assertThat(jdbc.queryForList(indices, "schema_v_tres", "schema_v_tres"))
                                .containsExactlyElementsOf(jdbc.queryForList(indices, referencia, referencia));
                assertThat(jdbc.queryForObject("select count(*) from tb_politicas", Long.class)).isZero();
                assertThat(jdbc.queryForObject("select count(*) from tb_politicas_privacidade", Long.class)).isZero();
                assertThat(jdbc.queryForObject("select count(*) from tb_suporte_controle where id = 1", Long.class))
                                .isEqualTo(1);
                assertThat(jdbc.queryForObject("select concluida from tb_inicializacao_administrador where id = 1",
                                Boolean.class)).isFalse();
                assertThat(flyway.info().current().getVersion())
                                .isEqualTo(flywayReferencia.info().current().getVersion());
                assertThat(flyway.info().pending()).isEmpty();
                assertThat(flyway.migrate().migrationsExecuted).isZero();
        }

        @TestConfiguration
        static class BancoLimpo {
                @Bean(initMethod = "migrate")
                Flyway flyway(DataSource fonte) {
                        new ResourceDatabasePopulator(new ClassPathResource("db/v3.sql")).execute(fonte);
                        return new FlywayConfig().flyway(fonte, true, "3");
                }

                @Bean
                static BeanFactoryPostProcessor migrarAntesDoJpa() {
                        return FlywayConfig.flywayBeforeJpa();
                }
        }
}
