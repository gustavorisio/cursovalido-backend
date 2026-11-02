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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
// O tópico e o aluno deste teste são removidos pelo rollback ao final da execução.
@Transactional
class ForumIntegracaoTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioForumRepositorio usuarios;
    @Autowired
    private TopicoRepositorio topicos;

    @Test
    void criarTopico_comAlunoAutenticado_devePersistirNoBanco() throws Exception {
        // O aluno Gustavo recebe um identificador próprio para esta execução.
        long idUsuario = System.nanoTime();
        String email = idUsuario + "@teste.local";
        usuarios.save(new UsuarioForum(idUsuario, "Gustavo", email, "ALUNO"));
        // O principal usa o perfil ALUNO exigido pela rota de criação de tópicos.
        UsuarioAutenticado principal = new UsuarioAutenticado(
                idUsuario, email, "SenhaAluno123!", "ALUNO", true, true, true);
        UsernamePasswordAuthenticationToken autenticacao = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());

        // O aluno publica uma dúvida sobre a consulta de tópicos, passando pelos filtros de segurança.
        mockMvc.perform(post("/api/forum/topicos")
                .with(authentication(autenticacao))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"titulo":"Como consultar meu topico no forum?","descricao":"Quero encontrar minha duvida na listagem de topicos."}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Como consultar meu topico no forum?"))
                .andExpect(jsonPath("$.idAutor").value(idUsuario));

        // A consulta pelo autor confirma que a dúvida foi persistida com o aluno correto.
        var topicosDoAluno = topicos.findByIdAutor(idUsuario);
        assertEquals(1, topicosDoAluno.size());
        assertEquals("Como consultar meu topico no forum?", topicosDoAluno.getFirst().getTitulo());
    }
}
