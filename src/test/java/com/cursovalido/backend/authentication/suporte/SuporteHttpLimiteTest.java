package com.cursovalido.backend.authentication.suporte;

import com.cursovalido.backend.authentication.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"aplicacao.suporte.email=suporte@example.com", "security.bcrypt.strength=10",
                "spring.datasource.url=jdbc:h2:mem:suporte-http;DB_CLOSE_DELAY=-1"})
@ActiveProfiles("test")
class SuporteHttpLimiteTest {
    @LocalServerPort int porta;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean EmailService emails;
    RestClient cliente;

    @BeforeEach
    void preparar() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS tb_suporte_controle (id INTEGER PRIMARY KEY)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS tb_suporte_limites (chave VARCHAR(80) PRIMARY KEY, expira_em TIMESTAMP WITH TIME ZONE NOT NULL)");
        jdbc.update("DELETE FROM tb_suporte_limites");
        jdbc.update("DELETE FROM tb_suporte_controle");
        jdbc.update("INSERT INTO tb_suporte_controle VALUES (1)");
        cliente = RestClient.builder().baseUrl("http://127.0.0.1:" + porta)
                .defaultStatusHandler(status -> true, (req, res) -> {}).build();
    }

    private ResponseEntity<String> enviar(String conta) {
        String json = """
                {"emailConta":"%s","usuario":"Maria","emailContato":"retorno@example.com",
                "motivo":"REVISAO_BANIMENTO","assunto":"Revisar bloqueio",
                "descricao":"Minha conta foi bloqueada por engano ontem."}
                """.formatted(conta);
        return cliente.post().uri("/api/suporte/solicitacoes").contentType(MediaType.APPLICATION_JSON)
                .body(json).retrieve().toEntity(String.class);
    }

    @Test
    void httpRealGravaLimiteEBloqueiaReenvioSemSegundoEmail() {
        assertThat(enviar("conta@example.com").getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tb_suporte_limites", Integer.class)).isEqualTo(3);
        var repetida = enviar("CONTA@example.com");
        assertThat(repetida.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(repetida.getBody()).contains("LIMITE_SUPORTE_ATINGIDO");
        assertThat(repetida.getHeaders().getFirst("Retry-After")).isNotBlank();
        verify(emails, times(1)).enviarSuporte(anyString(), anyString(), anyString(), anyString());
    }
}
