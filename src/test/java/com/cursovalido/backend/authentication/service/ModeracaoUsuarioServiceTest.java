package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import com.cursovalido.backend.authentication.dto.SuspensaoRequest;
import com.cursovalido.backend.authentication.entity.EscopoSuspensao;
import com.cursovalido.backend.authentication.entity.SuspensaoUsuario;
import com.cursovalido.backend.authentication.repository.SessaoRepository;
import com.cursovalido.backend.authentication.repository.SuspensaoUsuarioRepository;
import com.cursovalido.backend.authentication.validation.administracao.PrivilegiosAdministrativosService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModeracaoUsuarioServiceTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final SuspensaoUsuarioRepository suspensoes = mock(SuspensaoUsuarioRepository.class);
    private final IdentidadeExcluidaService identidades = mock(IdentidadeExcluidaService.class);
    private final SessaoRepository sessoes = mock(SessaoRepository.class);
    private final TokenEmailService tokens = mock(TokenEmailService.class);
    private final ModeracaoUsuarioService service = new ModeracaoUsuarioService(usuarios, identidades, suspensoes,
            mock(PrivilegiosAdministrativosService.class), mock(LogAuditoriaService.class), sessoes, tokens);

    @Test
    void deveConsiderarSuspensaoVigenteDeCadastroAnterior() {
        Usuario alvo = cadastrarAlvo();
        when(identidades.anteriores(alvo.getEmail())).thenReturn(List.of(3L));
        SuspensaoUsuario anterior = suspensao(3L, EscopoSuspensao.CONTA, null);
        when(suspensoes.findByIdUsuarioInAndRevogadaEmIsNullOrderByIdDesc(List.of(2L, 3L)))
                .thenReturn(List.of(anterior));

        assertTrue(service.restrito(2L, EscopoSuspensao.CONTA));
        assertTrue(service.mensagemBanimento(2L, EscopoSuspensao.CONTA).contains("permanentemente"));
    }

    @Test
    void deveExibirPrazoDeSuspensaoTemporaria() {
        cadastrarAlvo();
        Instant fim = Instant.now().plus(2, ChronoUnit.DAYS);
        when(suspensoes.findByIdUsuarioInAndRevogadaEmIsNullOrderByIdDesc(List.of(2L)))
                .thenReturn(List.of(suspensao(2L, EscopoSuspensao.COMENTARIOS, fim)));

        String mensagem = service.mensagemBanimento(2L, EscopoSuspensao.TOPICOS);

        assertTrue(mensagem.contains("temporariamente"));
        assertTrue(mensagem.contains("horário de Brasília"));
    }

    @Test
    void deveSubstituirSuspensaoDeConteudoERevogarAcessos() {
        Usuario alvo = cadastrarAlvo();
        var anterior = suspensao(2L, EscopoSuspensao.TOPICOS, null);
        when(suspensoes.findByIdUsuarioInAndRevogadaEmIsNullOrderByIdDesc(List.of(2L)))
                .thenReturn(List.of(anterior));

        var nova = service.aplicar(2L, new SuspensaoRequest(EscopoSuspensao.COMENTARIOS, " Ofensa ", null), 1L);

        assertEquals("Ofensa", nova.motivo());
        assertNotNull(anterior.getRevogadaEm());
        verify(suspensoes).save(any(SuspensaoUsuario.class));
        verify(sessoes).revogarPorUsuario(2L);
        verify(tokens).invalidarAcessos(alvo);
    }

    @Test
    void deveRevogarSuspensaoVinculadaAoUsuario() {
        cadastrarAlvo();
        var suspensao = suspensao(2L, EscopoSuspensao.FORUM, null);
        when(suspensoes.findById(5L)).thenReturn(Optional.of(suspensao));

        service.revogar(2L, 5L, 1L);

        assertNotNull(suspensao.getRevogadaEm());
    }

    private Usuario cadastrarAlvo() {
        Usuario usuario = new Usuario();
        usuario.setPerfil(Perfil.ALUNO);
        usuario.setEmail("aluno@teste.com");
        when(usuarios.findById(2L)).thenReturn(Optional.of(usuario));
        when(identidades.anteriores(usuario.getEmail())).thenReturn(List.of());
        return usuario;
    }

    private SuspensaoUsuario suspensao(Long idUsuario, EscopoSuspensao escopo, Instant fim) {
        SuspensaoUsuario suspensao = new SuspensaoUsuario();
        suspensao.setIdUsuario(idUsuario);
        suspensao.setEscopo(escopo);
        suspensao.setExpiraEm(fim);
        return suspensao;
    }
}

