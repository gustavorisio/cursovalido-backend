package com.cursovalido.backend.authentication;

import com.cursovalido.backend.authentication.configuration.AutenticacaoExceptionHandler;
import com.cursovalido.backend.authentication.validation.SenhaValidaValidator;
import com.cursovalido.backend.aulas.controller.CursoController;
import com.cursovalido.backend.aulas.service.CursoService;
import com.cursovalido.backend.forum.controller.ForumController;
import com.cursovalido.backend.forum.service.*;
import com.cursovalido.backend.noticias.controller.NoticiaController;
import com.cursovalido.backend.noticias.service.NoticiaService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ValidacaoModulosHttpTest {
    @Test
    void senhaExigeMaiusculaEspecialEComprimentoSemContarEspacoComoEspecial() {
        var regra = new SenhaValidaValidator();
        for (String senha : new String[] { "Abcdef!", "abcdefgh!", "Abcdefgh", "Abcdefg ", "A!" + "á".repeat(36) })
            assertFalse(regra.isValid(senha, null), senha);
        assertTrue(regra.isValid("Abcdefg!", null));
        assertTrue(regra.isValid("A!" + "a".repeat(70), null));
    }

    @Test
    void tituloAceita14ERejeita15Caracteres() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(new com.cursovalido.backend.forum.dto.TopicoRequest("a".repeat(14), "Texto"))
                    .isEmpty());
            assertFalse(validator.validate(new com.cursovalido.backend.forum.dto.TopicoRequest("a".repeat(15), "Texto"))
                    .isEmpty());
        }
    }

    @Test
    void camposInvalidosRetornamMesmoContratoEntreModulos() throws Exception {
        var forum = mock(TopicoService.class);
        var cursos = mock(CursoService.class);
        var noticias = mock(NoticiaService.class);
        var mvc = MockMvcBuilders.standaloneSetup(
                new ForumController(forum, mock(ComentarioService.class), mock(UsuarioForumService.class)),
                new CursoController(cursos), new NoticiaController(noticias))
                .setCustomArgumentResolvers(
                        new org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new AutenticacaoExceptionHandler()).build();
        for (String rota : new String[] { "/api/forum/topicos", "/api/cursos", "/api/noticias" }) {
            mvc.perform(post(rota).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("CAMPOS_INVALIDOS"))
                    .andExpect(jsonPath("$.campos.titulo").exists());
            mvc.perform(post(rota).contentType(MediaType.APPLICATION_JSON).content("{"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("JSON_INVALIDO"));
        }
        mvc.perform(get("/api/forum/topicos?page=abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.page").exists());
        mvc.perform(post("/api/forum/topicos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"123456789012345\",\"descricao\":\"Texto\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.titulo").value("O titulo deve ter no maximo 14 caracteres"));
        verifyNoInteractions(forum, cursos, noticias);
    }

    @Test
    void paginacaoInvalidaNaoConsultaRepositorio() {
        var repositorio = mock(com.cursovalido.backend.forum.repository.TopicoRepository.class);
        var servico = new TopicoService(repositorio, mock(UsuarioForumService.class), null);
        for (int[] valores : new int[][] { { -1, 5 }, { 0, 0 }, { 0, -1 }, { 0, 101 } }) {
            var erro = assertThrows(com.cursovalido.backend.forum.exception.RequisicaoInvalidaException.class,
                    () -> servico.listarRecentes(valores[0], valores[1], ""));
            assertEquals(400, erro.getStatusCode().value());
        }
        verifyNoInteractions(repositorio);
    }
}
