package com.cursovalido.backend.authentication.configuration;

import com.cursovalido.backend.authentication.entity.EscopoSuspensao;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import com.cursovalido.backend.authentication.service.ModeracaoUsuarioService;
import com.cursovalido.backend.authentication.validation.administracao.PrivilegiosAdministrativosService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModeracaoConfigurationTest {
    private final ModeracaoUsuarioService moderacao = mock(ModeracaoUsuarioService.class);
    private final PrivilegiosAdministrativosService privilegios = mock(PrivilegiosAdministrativosService.class);
    private HandlerInterceptor interceptador;

    @BeforeEach
    void preparar() {
        var registro = new Registro();
        new ModeracaoConfiguration(moderacao, privilegios).addInterceptors(registro);
        interceptador = (HandlerInterceptor) registro.interceptadores().getFirst();
        var aluno = new UsuarioAutenticado(1L, "a@x.com", "s", Perfil.ALUNO, true, true, true);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(aluno, null, List.of()));
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void contaSuspensaRecebeRespostaJsonValidaMesmoComAspasNoMotivo() throws Exception {
        when(moderacao.restrito(1L, EscopoSuspensao.CONTA)).thenReturn(true);
        when(moderacao.mensagemBanimento(1L, EscopoSuspensao.CONTA)).thenReturn("Motivo \"grave\"");
        var resposta = new MockHttpServletResponse();

        assertThat(interceptador.preHandle(new MockHttpServletRequest("GET", "/api/aluno/cursos"), resposta, null))
                .isFalse();
        assertThat(resposta.getStatus()).isEqualTo(403);
        assertThat(resposta.getContentAsString()).contains("\"codigo\":\"CONTA_SUSPENSA\"")
                .contains("Motivo \\\"grave\\\"");
    }

    @Test
    void restricaoDeComentariosUsaAMensagemDoProprioEscopo() throws Exception {
        when(moderacao.restrito(1L, EscopoSuspensao.COMENTARIOS)).thenReturn(true);
        when(moderacao.mensagemBanimento(1L, EscopoSuspensao.COMENTARIOS)).thenReturn("Comentarios bloqueados");
        var resposta = new MockHttpServletResponse();

        assertThat(interceptador.preHandle(new MockHttpServletRequest("POST", "/api/forum/topicos/5/comentarios"),
                resposta, null)).isFalse();
        assertThat(resposta.getContentAsString()).contains("COMENTARIOS_SUSPENSOS").contains("Comentarios bloqueados");
        verify(moderacao, never()).mensagemBanimento(1L, EscopoSuspensao.TOPICOS);
    }

    @Test
    void denunciaContinuaPermitidaComTopicosRestritos() throws Exception {
        when(moderacao.restrito(1L, EscopoSuspensao.TOPICOS)).thenReturn(true);

        assertThat(interceptador.preHandle(new MockHttpServletRequest("POST", "/api/forum/topicos/5/denuncias"),
                new MockHttpServletResponse(), null)).isTrue();
        assertThat(interceptador.preHandle(new MockHttpServletRequest("POST", "/api/forum/topicos"),
                new MockHttpServletResponse(), null)).isFalse();
    }

    @Test
    void conviteExigeAdministradorPrincipalERotasLivresPassam() throws Exception {
        assertThat(interceptador.preHandle(new MockHttpServletRequest("POST", "/api/administracao/convites"),
                new MockHttpServletResponse(), null)).isTrue();
        verify(privilegios).verificarPrincipal(1L);
        assertThat(interceptador.preHandle(new MockHttpServletRequest("POST", "/api/autenticacao/logout"),
                new MockHttpServletResponse(), null)).isTrue();
        SecurityContextHolder.clearContext();
        assertThat(interceptador.preHandle(new MockHttpServletRequest("GET", "/api/aluno/cursos"),
                new MockHttpServletResponse(), null)).isTrue();
    }

    private static class Registro extends InterceptorRegistry {
        List<Object> interceptadores() {
            return getInterceptors();
        }
    }
}
