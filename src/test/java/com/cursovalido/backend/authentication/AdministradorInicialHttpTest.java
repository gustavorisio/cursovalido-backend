package com.cursovalido.backend.authentication;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.service.AdministradorInicialService;
import com.cursovalido.backend.authentication.service.EmailService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
                "spring.datasource.url=jdbc:h2:mem:admin_inicio;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "aplicacao.admin.senha=SenhaAdminTeste123!",
                "aplicacao.admin.email=primeiro-admin@example.com",
                "security.bcrypt.strength=10"
})
@ActiveProfiles("test")
class AdministradorInicialHttpTest {
        @LocalServerPort
        private int porta;
        @Autowired
        private UsuarioRepository usuarios;
        @Autowired
        private PasswordEncoder senhas;
        @MockitoBean
        private EmailService emails;

        @Test
        void administradorCriadoNaInicializacaoAceitaSenhaConfigurada() {
                var administrador = usuarios.findByEmail("primeiro-admin@example.com").orElseThrow();
                assertEquals(Perfil.ADMINISTRADOR, administrador.getPerfil());
                assertTrue(senhas.matches("SenhaAdminTeste123!", administrador.getSenha()));

                var cliente = RestClient.builder().baseUrl("http://localhost:" + porta)
                                .defaultStatusHandler(status -> true, (requisicao, resposta) -> {
                                }).build();
                var resposta = cliente.post().uri("/api/autenticacao/login")
                                .body(Map.of("email", "primeiro-admin@example.com", "senha", "SenhaAdminTeste123!"))
                                .retrieve().toEntity(Map.class);

                assertEquals(200, resposta.getStatusCode().value());
                assertNull(resposta.getBody().get("token"));
                var codigo = ArgumentCaptor.forClass(String.class);
                Mockito.verify(emails).enviarCodigo2fa(ArgumentMatchers.any(),
                                codigo.capture());
                resposta = cliente.post().uri("/api/autenticacao/confirmar-2fa")
                                .body(Map.of("email", "primeiro-admin@example.com", "codigo", codigo.getValue()))
                                .retrieve().toEntity(Map.class);
                assertEquals(200, resposta.getStatusCode().value());
                assertNotNull(resposta.getBody().get("token"));
        }
}
