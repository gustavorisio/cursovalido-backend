package com.cursovalido.backend.forum.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cursovalido.backend.forum.entity.UsuarioForum;
import com.cursovalido.backend.forum.service.ComentarioServico;
import com.cursovalido.backend.forum.service.TopicoServico;
import com.cursovalido.backend.forum.service.UsuarioForumServico;
import com.cursovalido.backend.authentication.service.TokenJwtServico;
import com.cursovalido.backend.authentication.service.UsuarioDetalhesServico;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.ResponseEntity;

@WebMvcTest(ControladorForum.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ControladorForumTest.ErroDeValidacaoTestConfig.class)
class ControladorForumTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TopicoServico topicos;
    @MockitoBean
    private ComentarioServico comentarios;
    @MockitoBean
    private UsuarioForumServico usuarios;
    @MockitoBean
    private TokenJwtServico tokens;
    @MockitoBean
    private UsuarioDetalhesServico usuariosAutenticacao;

    @Test
    void deveListarUsuariosAtivosComRespostaJson() throws Exception {
        when(usuarios.listarAtivos())
                .thenReturn(List.of(new UsuarioForum(7L, "Aluno", "aluno@teste.com", "ALUNO")));

        mockMvc.perform(get("/api/forum/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value[0].id").value(7))
                .andExpect(jsonPath("$.value[0].nome").value("Aluno"))
                .andExpect(jsonPath("$.value[0].perfil").value("ALUNO"));
    }

    @Test
    void deveRetornarErro4xxQuandoCriacaoDeTopicoForInvalida() throws Exception {
        // Campos vazios
        mockMvc.perform(post("/api/forum/topicos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"titulo":"","descricao":""}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("requisicao_invalida"));
    }

    @RestControllerAdvice
    static class ErroDeValidacaoTestConfig {
        @ExceptionHandler(MethodArgumentNotValidException.class)
        ResponseEntity<Map<String, Object>> tratarValidacao() {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", 400, "erro", "requisicao_invalida"));
        }
    }
}
