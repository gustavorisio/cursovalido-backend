package com.cursovalido.backend.forum.service;

import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import com.cursovalido.backend.forum.entity.Topico;
import com.cursovalido.backend.forum.entity.UsuarioForum;
import com.cursovalido.backend.forum.exception.AcessoNegadoException;
import com.cursovalido.backend.forum.exception.RecursoNaoEncontradoException;
import com.cursovalido.backend.forum.repository.TopicoRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopicoServiceTest {

    @Mock
    private TopicoRepository repositorio;
    @Mock
    private UsuarioForumService usuarios;
    @Mock
    private LogAuditoriaService auditoria;
    @InjectMocks
    private TopicoService servico;

    private UsuarioForum usuario;

    @BeforeEach
    void configurarCenario() {
        // O mesmo usuário ativo é usado nos cenários que dependem de autoria.
        usuario = new UsuarioForum(7L, "Aluno", "aluno@teste.com", "ALUNO");
    }

    @Test
    void deveCriarTopicoERegistrarAuditoria() {
        Topico topico = new Topico();
        topico.setTitulo("Java");
        topico.setDescricao("Duvida");
        when(usuarios.bloquearAtivo(7L)).thenReturn(usuario);
        when(repositorio.save(topico)).thenReturn(topico);

        Topico resultado = servico.criar(topico, 7L);

        assertEquals(7L, resultado.getIdAutor());
        verify(repositorio).save(topico);
        verify(auditoria).registrar(eq(7L), eq("CADASTRO_TOPICO"), any(String.class), eq(null));
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioNaoForAutor() {
        Topico topico = new Topico();
        topico.setIdAutor(99L);
        // O serviço deve negar a edição sem chegar ao repositório de gravação.
        when(usuarios.buscarAtivo(7L)).thenReturn(usuario);
        when(repositorio.findById(10L)).thenReturn(Optional.of(topico));

        assertThrows(AcessoNegadoException.class,
                () -> servico.editar(10L, new Topico(), 7L));
        verify(repositorio, never()).save(any(Topico.class));
    }

    @Test
    void deveLancarExcecaoQuandoTopicoEstiverArquivado() {
        Topico topico = new Topico();
        topico.setAtivo(false);
        when(repositorio.findById(10L)).thenReturn(Optional.of(topico));

        assertThrows(RecursoNaoEncontradoException.class, () -> servico.buscarAtivo(10L));
    }

    @Test
    void deveListarTopicosComBuscaSemEspacosNasExtremidades() {
        when(repositorio.listarRecentes(any(), eq("java"))).thenReturn(List.of());

        servico.listarRecentes(0, 5, "  java  ", 7L);

        verify(repositorio).listarRecentes(any(), eq("java"));
        verify(auditoria).registrar(7L, "CONSULTA_TOPICOS", "java", null);
    }

    @Test
    void listarRecentes_semBusca_deveConsultarPaginaComTermoVazio() {
        when(repositorio.listarRecentes(PageRequest.of(1, 3), "")).thenReturn(List.of());
        assertThat(servico.listarRecentes(1, 3)).isEmpty();
        verify(repositorio).listarRecentes(PageRequest.of(1, 3), "");
    }

    @Test
    void listarRecentes_comBuscaNula_deveNormalizarTermo() {
        when(repositorio.listarRecentes(PageRequest.of(0, 5), "")).thenReturn(List.of());
        assertThat(servico.listarRecentes(0, 5, null)).isEmpty();
        verify(repositorio).listarRecentes(PageRequest.of(0, 5), "");
    }

    @Test
    void criar_comDadosDoCliente_deveDefinirEstadoInicialEAutorAutenticado() {
        Topico topico = new Topico();
        topico.setId(10L);
        topico.setIdAutor(99L);
        topico.setAtivo(false);
        topico.setFechado(true);
        topico.setQuantidadeRespostas(12);
        when(usuarios.bloquearAtivo(7L)).thenReturn(usuario);
        when(repositorio.save(topico)).thenReturn(topico);
        var antes = LocalDateTime.now(ZoneOffset.UTC);
        Topico resultado = servico.criar(topico, 7L);
        assertThat(resultado.getIdAutor()).isEqualTo(7L);
        assertThat(resultado.getUsuarioAutor()).isSameAs(usuario);
        assertThat(resultado.getCriadoEm()).isBetween(antes, LocalDateTime.now(ZoneOffset.UTC));
        assertThat(resultado.isAtivo()).isTrue();
        assertThat(resultado.isFechado()).isFalse();
        assertThat(resultado.getQuantidadeRespostas()).isZero();
    }

    @Test
    void editar_comAutor_deveSalvarTituloDescricaoERegistrarAuditoria() {
        Topico topico = prepararTopico(7L);
        Topico dados = new Topico();
        dados.setTitulo("Novo titulo");
        dados.setDescricao("Nova descricao");
        when(repositorio.save(topico)).thenReturn(topico);
        assertThat(servico.editar(10L, dados, 7L)).isSameAs(topico);
        assertThat(topico.getTitulo()).isEqualTo("Novo titulo");
        assertThat(topico.getDescricao()).isEqualTo("Nova descricao");
        assertThat(topico.getIdAutor()).isEqualTo(7L);
        verify(auditoria).registrar(7L, "EDICAO_TOPICO", "Topico 10", null);
    }

    @Test
    void editar_comAutorNulo_deveNegarEdicao() {
        prepararTopico(null);
        assertThrows(AcessoNegadoException.class, () -> servico.editar(10L, new Topico(), 7L));
        verify(repositorio, never()).save(any());
    }

    @Test
    void buscarAtivo_comIdInexistente_deveRetornarNaoEncontrado() {
        when(repositorio.findById(10L)).thenReturn(Optional.empty());
        assertThrows(RecursoNaoEncontradoException.class, () -> servico.buscarAtivo(10L));
    }

    @Test
    void buscarAtivo_comTopicoAtivo_deveRetornarTopico() {
        Topico topico = new Topico();
        when(repositorio.findById(10L)).thenReturn(Optional.of(topico));
        assertThat(servico.buscarAtivo(10L)).isSameAs(topico);
    }

    @ParameterizedTest
    @ValueSource(strings = { "arquivar", "fechar" })
    void alterarEstado_comProfessorAutor_devePersistirERegistrarAuditoria(String operacao) {
        usuario.setPerfil("PROFESSOR");
        Topico topico = prepararTopico(7L);
        alterarEstado(operacao);
        conferirEstado(topico, operacao);
        verify(repositorio).save(topico);
        verify(auditoria).registrar(7L, operacao.equals("arquivar") ? "ARQUIVAMENTO_TOPICO" : "FECHAMENTO_TOPICO",
                "Topico 10", null);
    }

    @ParameterizedTest
    @ValueSource(strings = { "arquivar", "fechar" })
    void alterarEstado_comAdministrador_devePermitirAlterarOutroAutor(String operacao) {
        usuario.setPerfil("ADMINISTRADOR");
        Topico topico = prepararTopico(99L);
        when(usuarios.ehAdministrador(usuario)).thenReturn(true);
        alterarEstado(operacao);
        conferirEstado(topico, operacao);
        verify(repositorio).save(topico);
    }

    @ParameterizedTest
    @ValueSource(strings = { "arquivar", "fechar" })
    void alterarEstado_comOutroUsuario_deveNegarSemPersistir(String operacao) {
        prepararTopico(99L);
        assertThrows(AcessoNegadoException.class, () -> alterarEstado(operacao));
        verify(repositorio, never()).save(any());
        verify(auditoria, never()).registrar(any(), any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = { "arquivar", "fechar" })
    void alunoNaoPodeAlterarEstadoNemDoProprioTopico(String operacao) {
        prepararTopico(7L);
        assertThrows(AcessoNegadoException.class, () -> alterarEstado(operacao));
        verify(repositorio, never()).save(any());
    }

    private Topico prepararTopico(Long autorId) {
        Topico topico = new Topico();
        topico.setId(10L);
        topico.setIdAutor(autorId);
        when(usuarios.buscarAtivo(7L)).thenReturn(usuario);
        when(repositorio.findById(10L)).thenReturn(Optional.of(topico));
        return topico;
    }

    private void alterarEstado(String operacao) {
        if (operacao.equals("arquivar"))
            servico.arquivar(10L, 7L);
        else
            servico.fechar(10L, 7L);
    }

    private void conferirEstado(Topico topico, String operacao) {
        if (operacao.equals("arquivar"))
            assertThat(topico.isAtivo()).isFalse();
        else
            assertThat(topico.isFechado()).isTrue();
    }

    @Test
    void topicoComOfensaESalvoComAsteriscos() {
        Topico topico = new Topico();
        topico.setTitulo("Duvida");
        topico.setDescricao("que porra de erro");
        when(usuarios.bloquearAtivo(7L)).thenReturn(usuario);
        when(repositorio.save(topico)).thenReturn(topico);
        assertEquals("que *** de erro", servico.criar(topico, 7L).getDescricao());
        verify(auditoria).registrar(7L, "CONTEUDO_OFENSIVO_OCULTADO", "Topico", null);
    }

    @Test
    void edicaoOcultaOfensaNoTitulo() {
        Topico existente = new Topico();
        existente.setId(3L);
        existente.setIdAutor(7L);
        Topico dados = new Topico();
        dados.setTitulo("seu idiota");
        dados.setDescricao("Texto");
        when(usuarios.buscarAtivo(7L)).thenReturn(usuario);
        when(repositorio.findById(3L)).thenReturn(Optional.of(existente));
        when(repositorio.save(existente)).thenReturn(existente);
        assertEquals("seu ***", servico.editar(3L, dados, 7L).getTitulo());
    }

    @Test
    void limiteDeTresMinutosImpedeSalvarOutroTopico() {
        when(usuarios.bloquearAtivo(7L)).thenReturn(usuario);
        when(repositorio.existsByIdAutorAndCriadoEmAfter(eq(7L), any())).thenReturn(true);
        var inicio = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(3);
        var erro = assertThrows(ResponseStatusException.class,
                () -> servico.criar(new Topico(), 7L));
        assertEquals(429, erro.getStatusCode().value());
        assertThat(erro.getReason()).contains("3 minutos");
        var limite = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repositorio).existsByIdAutorAndCriadoEmAfter(eq(7L), limite.capture());
        assertThat(limite.getValue()).isBetween(inicio, LocalDateTime.now(ZoneOffset.UTC).minusMinutes(3));
        verify(repositorio, never()).save(any());
    }
}
