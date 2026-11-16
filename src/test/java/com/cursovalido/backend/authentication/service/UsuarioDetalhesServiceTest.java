package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import java.util.Optional;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.StatusAprovacao;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsuarioDetalhesServiceTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final ResponsabilidadeService responsabilidade = mock(ResponsabilidadeService.class);
    private final UsuarioDetalhesService service = new UsuarioDetalhesService(usuarios, responsabilidade);

    @Test
    void deveCarregarContaAprovadaComoHabilitada() {
        Usuario usuario = cadastrarUsuario(StatusAprovacao.APROVADO);
        usuario.setAceitouTermosLgpd(true);
        when(responsabilidade.atualizado(usuario)).thenReturn(true);

        var detalhes = service.loadUserByUsername(usuario.getEmail());

        assertEquals(usuario.getEmail(), detalhes.getUsername());
        assertTrue(detalhes.isEnabled());
    }

    @Test
    void deveCarregarContaPendenteComoDesabilitada() {
        Usuario usuario = cadastrarUsuario(StatusAprovacao.PENDENTE_APROVACAO);

        assertFalse(service.loadUserByUsername(usuario.getEmail()).isEnabled());
    }

    @Test
    void deveRecusarEmailDesconhecido() {
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("outro@teste.com"));
    }

    private Usuario cadastrarUsuario(StatusAprovacao status) {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 1L);
        usuario.setEmail("aluno@teste.com");
        usuario.setPerfil(Perfil.ALUNO);
        usuario.setAtivo(true);
        usuario.setStatusAprovacao(status);
        when(usuarios.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        return usuario;
    }
}


