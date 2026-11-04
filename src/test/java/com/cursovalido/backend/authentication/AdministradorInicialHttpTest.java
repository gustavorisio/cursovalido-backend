package com.cursovalido.backend.authentication;

import static org.junit.jupiter.api.Assertions.*;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.repository.UsuarioRepositorio;
import com.cursovalido.backend.authentication.service.AdministradorInicialServico;
import com.cursovalido.backend.authentication.service.EmailServico;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_inicio;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "aplicacao.admin.senha=SenhaAdminTeste123!",
        "aplicacao.admin.email=primeiro-admin@example.com",
        "security.bcrypt.strength=10"
})
@ActiveProfiles("test")
class AdministradorInicialHttpTest {
    @LocalServerPort private int porta;
    @Autowired private UsuarioRepositorio usuarios;
    @Autowired private PasswordEncoder senhas;
    @MockitoBean private EmailServico emails;

    @Test
    void administradorCriadoNaInicializacaoAceitaSenhaConfigurada() {
        var administrador = usuarios.findByEmail("primeiro-admin@example.com").orElseThrow();
        assertEquals(Perfil.ADMINISTRADOR, administrador.getPerfil());
        assertTrue(senhas.matches("SenhaAdminTeste123!", administrador.getSenha()));

        var cliente = RestClient.builder().baseUrl("http://localhost:" + porta)
                .defaultStatusHandler(status -> true, (requisicao, resposta) -> {}).build();
        var resposta = cliente.post().uri("/api/autenticacao/login")
                .body(Map.of("email", "primeiro-admin@example.com", "senha", "SenhaAdminTeste123!"))
                .retrieve().toEntity(Map.class);

        assertEquals(403, resposta.getStatusCode().value());
        assertEquals("TERMOS_NAO_ACEITOS", resposta.getBody().get("codigo"));
        assertNotNull(resposta.getBody().get("token"));
    }
}
