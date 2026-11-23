package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.dto.SuspensaoRequest;
import com.cursovalido.backend.authentication.dto.UsuarioAdministracaoResposta;
import com.cursovalido.backend.authentication.entity.EscopoSuspensao;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.SuspensaoUsuario;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.SessaoRepository;
import com.cursovalido.backend.authentication.repository.SuspensaoUsuarioRepository;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.validation.administracao.PrivilegiosAdministrativosService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModeracaoUsuarioServiceTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final SuspensaoUsuarioRepository suspensoes = mock(SuspensaoUsuarioRepository.class);
    private final IdentidadeExcluidaService identidades = mock(IdentidadeExcluidaService.class);
    private final SessaoRepository sessoes = mock(SessaoRepository.class);
    private final TokenEmailService tokens = mock(TokenEmailService.class);
    private final PrivilegiosAdministrativosService privilegios = mock(PrivilegiosAdministrativosService.class);
    private final ModeracaoUsuarioService service = new ModeracaoUsuarioService(usuarios, identidades, suspensoes,
            privilegios, mock(LogAuditoriaService.class), sessoes, tokens);

    @Test
    void devePermitirAoPrincipalExcluirProfessor() {
        Usuario principal = usuario(1L, Perfil.ADMINISTRADOR);
        Usuario professor = usuario(2L, Perfil.PROFESSOR);
        when(privilegios.verificarAdministrador(1L)).thenReturn(principal);
        when(privilegios.ehPrincipal(principal)).thenReturn(true);
        when(usuarios.findByExcluidoEmIsNull()).thenReturn(List.of(principal, professor));

        var resultado = service.listarUsuarios(1L, null);

        assertEquals(List.of(1L, 2L), resultado.stream().map(UsuarioAdministracaoResposta::id).toList());
        assertFalse(resultado.getFirst().podeModerar());
        assertTrue(resultado.getLast().podeExcluir());
    }

    @Test
    void deveImpedirAdministradorComumDeModerarOutroAdministrador() {
        Usuario conta = usuario(1L, Perfil.ADMINISTRADOR);
        Usuario alvo = usuario(2L, Perfil.ADMINISTRADOR);
        when(privilegios.verificarAdministrador(1L)).thenReturn(conta);
        when(usuarios.findByExcluidoEmIsNull()).thenReturn(List.of(alvo));

        var resultado = service.listarUsuarios(1L, null).getFirst();

        assertFalse(resultado.podeModerar());
        assertFalse(resultado.podeExcluir());
    }

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
    void administradorComumPodeModerarProfessorEAlunoMasNaoExcluir() {
        Usuario administrador = usuario(1L, Perfil.ADMINISTRADOR);
        when(privilegios.verificarAdministrador(1L)).thenReturn(administrador);
        when(usuarios.findByExcluidoEmIsNull()).thenReturn(List.of(
                usuario(2L, Perfil.PROFESSOR), usuario(3L, Perfil.ALUNO)));

        var resultado = service.listarUsuarios(1L, null);

        assertEquals(2, resultado.size());
        resultado.forEach(alvo -> {
            assertTrue(alvo.podeModerar());
            assertFalse(alvo.podeExcluir());
        });
    }

    @Test
    void deveExibirPrazoDeSuspensaoTemporaria() {
        cadastrarAlvo();
        Instant fim = Instant.now().plus(2, ChronoUnit.DAYS);
        when(suspensoes.findByIdUsuarioInAndRevogadaEmIsNullOrderByIdDesc(List.of(2L)))
                .thenReturn(List.of(suspensao(2L, EscopoSuspensao.FORUM, fim)));

        String mensagem = service.mensagemBanimento(2L, EscopoSuspensao.FORUM);

        assertTrue(mensagem.contains("temporariamente"));
        assertTrue(mensagem.contains("horário de Brasília"));
    }

    @Test
    void deveSubstituirSuspensaoDoForumERevogarAcessos() {
        autenticarAdministradorComum();
        Usuario alvo = cadastrarAlvo();
        alvo.setPerfil(Perfil.PROFESSOR);
        var anterior = suspensao(2L, EscopoSuspensao.FORUM, null);
        when(suspensoes.findByIdUsuarioInAndRevogadaEmIsNullOrderByIdDesc(List.of(2L)))
                .thenReturn(List.of(anterior));

        var nova = service.aplicar(2L, new SuspensaoRequest(EscopoSuspensao.FORUM, " Ofensa ", null), 1L);

        assertEquals("Ofensa", nova.motivo());
        assertNotNull(anterior.getRevogadaEm());
        verify(suspensoes).save(any(SuspensaoUsuario.class));
        verify(sessoes).revogarPorUsuario(2L);
        verify(tokens).invalidarAcessos(alvo);
    }

    @Test
    void deveRevogarSuspensaoVinculadaAoUsuario() {
        autenticarAdministradorComum();
        cadastrarAlvo();
        var suspensao = suspensao(2L, EscopoSuspensao.FORUM, null);
        when(suspensoes.findById(5L)).thenReturn(Optional.of(suspensao));

        service.revogar(2L, 5L, 1L);

        assertNotNull(suspensao.getRevogadaEm());
    }

    private void autenticarAdministradorComum() {
        Usuario administrador = new Usuario();
        ReflectionTestUtils.setField(administrador, "id", 1L);
        administrador.setPerfil(Perfil.ADMINISTRADOR);
        when(privilegios.verificarAdministrador(1L)).thenReturn(administrador);
    }

    private Usuario cadastrarAlvo() {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 2L);
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

    private Usuario usuario(Long id, Perfil perfil) {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", id);
        usuario.setPerfil(perfil);
        usuario.setEmail("usuario" + id + "@example.com");
        return usuario;
    }

}
