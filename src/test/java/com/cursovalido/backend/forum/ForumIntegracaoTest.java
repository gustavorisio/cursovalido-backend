package com.cursovalido.backend.forum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import com.cursovalido.backend.forum.entity.UsuarioForum;
import com.cursovalido.backend.forum.repository.TopicoRepositorio;
import com.cursovalido.backend.forum.repository.UsuarioForumRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ForumIntegracaoTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioForumRepositorio usuarios;
    @Autowired
    private TopicoRepositorio topicos;

    @Test
    void deveCriarTopicoPelaApiEPersistirNoBanco() throws Exception {
        usuarios.save(new UsuarioForum(7L, "Aluno", "aluno@teste.com", "ALUNO"));
        UsuarioAutenticado principal = new UsuarioAutenticado(
                7L, "aluno@teste.com", "senha", "ALUNO", true, true, true);
        var autenticacao = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());

        // A requisição valida segurança, controlador, serviço e persistência.
        mockMvc.perform(post("/api/forum/topicos")
                .with(authentication(autenticacao))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"titulo":"Fluxo completo","descricao":"Criado pela API"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Fluxo completo"))
                .andExpect(jsonPath("$.idAutor").value(7));

        assertEquals(1, topicos.count());
        assertEquals("Fluxo completo", topicos.findAll().getFirst().getTitulo());
    }
}
