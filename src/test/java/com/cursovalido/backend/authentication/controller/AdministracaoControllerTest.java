package com.cursovalido.backend.authentication.controller;

import com.cursovalido.backend.authentication.dto.*;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.service.*;
import com.cursovalido.backend.authentication.validation.administracao.PrivilegiosAdministrativosService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static com.cursovalido.backend.authentication.support.UsuarioAutenticadoFixture.usuario;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdministracaoControllerTest {
    private final ConviteAcessoService convites = mock(ConviteAcessoService.class);
    private final LogAuditoriaService auditoria = mock(LogAuditoriaService.class);
    private final PrivilegiosAdministrativosService privilegios = mock(PrivilegiosAdministrativosService.class);
    private final AdministracaoController controlador = new AdministracaoController(convites, auditoria, privilegios);

    @Test
    void integridadeExigePrincipalAntesDeConsultarAuditoria() {
        when(auditoria.verificarIntegridade()).thenReturn(true);
        assertThat(controlador.verificarIntegridadeAuditoria(usuario())).containsEntry("integra", true);
        var ordem = inOrder(privilegios, auditoria);
        ordem.verify(privilegios).verificarPrincipal(7L);
        ordem.verify(auditoria).verificarIntegridade();
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(privilegios).verificarPrincipal(7L);
        clearInvocations(auditoria);
        assertThatThrownBy(() -> controlador.verificarIntegridadeAuditoria(usuario()))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(auditoria);
    }

    @Test
    void conviteAprovacaoEReenvioMantemAdministradorAutenticado() {
        var dados = new ConviteAcessoRequest("professor@example.com", Perfil.PROFESSOR);
        controlador.criar(usuario(), dados);
        controlador.aprovar(50L, usuario());
        controlador.reenviar(50L, usuario());
        verify(convites).criar("professor@example.com", Perfil.PROFESSOR, 7L);
        verify(convites).aprovar(50L, 7L);
        verify(convites).reenviar(50L, 7L);
    }

    @Test
    void consultaDeConvitesUsaUsuarioAutenticado() {
        var convite = mock(ConviteResumoResponse.class);
        when(convites.convitesPendentes(7L)).thenReturn(List.of(convite));
        assertThat(controlador.listarConvites(usuario())).containsExactly(convite);
    }
}
