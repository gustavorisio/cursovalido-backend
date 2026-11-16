package com.cursovalido.backend.authentication.validation.administracao;

import com.cursovalido.backend.authentication.entity.InicializacaoAdministrador;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.InicializacaoAdministradorRepository;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrivilegiosAdministrativosServiceTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final InicializacaoAdministradorRepository inicializacao = mock(InicializacaoAdministradorRepository.class);
    private final PrivilegiosAdministrativosService service = new PrivilegiosAdministrativosService(usuarios, inicializacao);

    @Test
    void devePermitirOperacaoExclusivaAoAdministradorPrincipal() {
        cadastrarUsuario(Perfil.ADMINISTRADOR, true);
        var registro = new InicializacaoAdministrador();
        registro.concluir(1L);
        when(inicializacao.findById(1L)).thenReturn(Optional.of(registro));

        assertDoesNotThrow(() -> service.verificarPrincipal(1L));
    }

    @Test
    void deveRecusarOperacaoExclusivaParaOutroAdministrador() {
        cadastrarUsuario(Perfil.ADMINISTRADOR, true);
        var registro = new InicializacaoAdministrador();
        registro.concluir(2L);
        when(inicializacao.findById(1L)).thenReturn(Optional.of(registro));

        var erro = assertThrows(ResponseStatusException.class, () -> service.verificarPrincipal(1L));

        assertEquals(HttpStatus.FORBIDDEN, erro.getStatusCode());
    }

    @Test
    void deveRecusarProfessorEmOperacaoAdministrativa() {
        cadastrarUsuario(Perfil.PROFESSOR, true);

        var erro = assertThrows(ResponseStatusException.class, () -> service.verificarAdministrador(1L));

        assertEquals(HttpStatus.FORBIDDEN, erro.getStatusCode());
    }

    @Test
    void deveRecusarAdministradorInativo() {
        cadastrarUsuario(Perfil.ADMINISTRADOR, false);

        var erro = assertThrows(ResponseStatusException.class, () -> service.verificarAdministrador(1L));

        assertEquals(HttpStatus.FORBIDDEN, erro.getStatusCode());
    }

    private void cadastrarUsuario(Perfil perfil, boolean ativo) {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 1L);
        usuario.setPerfil(perfil);
        usuario.setAtivo(ativo);
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario));
    }
}

