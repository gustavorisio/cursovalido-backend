package com.cursovalido.backend.authentication.controller;

import com.cursovalido.backend.authentication.dto.SuspensaoRequest;
import com.cursovalido.backend.authentication.entity.EscopoSuspensao;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import com.cursovalido.backend.authentication.service.ContaUsuarioService;
import com.cursovalido.backend.authentication.service.ModeracaoUsuarioService;
import com.cursovalido.backend.authentication.validation.administracao.PrivilegiosAdministrativosService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModeracaoControllerTest {
    private final PrivilegiosAdministrativosService privilegios = mock(PrivilegiosAdministrativosService.class);
    private final ModeracaoUsuarioService moderacao = mock(ModeracaoUsuarioService.class);
    private final ContaUsuarioService contas = mock(ContaUsuarioService.class);
    private final ModeracaoController controller = new ModeracaoController(privilegios, moderacao, contas);
    private final UsuarioAutenticado administrador = new UsuarioAutenticado(
            1L, "admin@example.com", "hash", Perfil.ADMINISTRADOR, true, true, true);

    @Test
    void deveInformarQuandoAdministradorForPrincipal() {
        Usuario conta = conta(1L, Perfil.ADMINISTRADOR);
        when(privilegios.verificarAdministrador(1L)).thenReturn(conta);
        when(privilegios.ehPrincipal(conta)).thenReturn(true);

        var permissoes = controller.permissoes(administrador);

        assertEquals(1L, permissoes.get("idUsuario"));
        assertEquals(true, permissoes.get("principal"));
    }

    @Test
    void deveDelegarListagemComFiltroAoServico() {
        controller.listar(administrador, true);

        verify(moderacao).listarUsuarios(1L, true);
    }

    @Test
    void deveAplicarSuspensaoEmNomeDoAdministradorAutenticado() {
        var dados = new SuspensaoRequest(EscopoSuspensao.FORUM, "Ofensa", null);

        controller.aplicar(2L, dados, administrador);

        verify(moderacao).aplicar(2L, dados, 1L);
    }

    @Test
    void deveRevogarSuspensaoEmNomeDoAdministradorAutenticado() {
        controller.revogar(2L, 5L, administrador);

        verify(moderacao).revogar(2L, 5L, 1L);
    }

    private Usuario conta(Long id, Perfil perfil) {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", id);
        usuario.setPerfil(perfil);
        usuario.setEmail("usuario" + id + "@example.com");
        return usuario;
    }
}
