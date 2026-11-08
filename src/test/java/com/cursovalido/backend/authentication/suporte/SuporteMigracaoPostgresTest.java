package com.cursovalido.backend.authentication.suporte;

import com.cursovalido.backend.forum.support.PostgresTeste;
import java.util.HashMap;
import java.util.function.Supplier;
import java.util.concurrent.atomic.AtomicInteger;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(PostgresTeste.CicloDeVida.class)
class SuporteMigracaoPostgresTest {
    @Test
    void v1EmBancoNovoCriaLimiteEBloqueiaSegundoEnvio() {
        var propriedades = new HashMap<String, Supplier<?>>();
        PostgresTeste.configurar(propriedades::put, "suporte_migracao");
        var fonte = new DriverManagerDataSource(
                propriedades.get("spring.datasource.url").get().toString(),
                propriedades.get("spring.datasource.username").get().toString(),
                propriedades.get("spring.datasource.password").get().toString());
        var flyway = Flyway.configure().dataSource(fonte).locations("classpath:db/migration").load();
        flyway.migrate();
        assertThat(flyway.info().applied()).hasSize(1);
        var jdbc = new JdbcTemplate(fonte);
        var servico = new LimiteSuporteService(jdbc, new DataSourceTransactionManager(fonte));
        var envios = new AtomicInteger();
        servico.enviar("127.0.0.1", "conta@example.com", "retorno@example.com", envios::incrementAndGet);
        assertThatThrownBy(() -> servico.enviar("127.0.0.2", "CONTA@example.com", "outro@example.com",
                envios::incrementAndGet)).isInstanceOf(LimiteSuporteException.class);
        assertThat(envios.get()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tb_suporte_limites", Integer.class)).isEqualTo(3);
    }
}
