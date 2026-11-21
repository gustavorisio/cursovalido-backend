package com.cursovalido.backend.forum.service;

import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import com.cursovalido.backend.authentication.validation.administracao.PrivilegiosAdministrativosService;
import com.cursovalido.backend.forum.dto.DenunciaDtos.DecisaoDenunciaRequest;
import com.cursovalido.backend.forum.dto.DenunciaDtos.DenunciaRequest;
import com.cursovalido.backend.forum.entity.Comentario;
import com.cursovalido.backend.forum.entity.DenunciaForum;
import com.cursovalido.backend.forum.entity.MotivoDenuncia;
import com.cursovalido.backend.forum.entity.StatusDenuncia;
import com.cursovalido.backend.forum.entity.TipoConteudoForum;
import com.cursovalido.backend.forum.entity.Topico;
import com.cursovalido.backend.forum.entity.UsuarioForum;
import com.cursovalido.backend.forum.exception.ConflitoException;
import com.cursovalido.backend.forum.exception.RecursoNaoEncontradoException;
import com.cursovalido.backend.forum.exception.RequisicaoInvalidaException;
import com.cursovalido.backend.forum.repository.ComentarioRepository;
import com.cursovalido.backend.forum.repository.DenunciaForumRepository;
import com.cursovalido.backend.forum.repository.TopicoRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DenunciaForumServiceTest {
    private final DenunciaForumRepository denuncias = mock(DenunciaForumRepository.class);
    private final TopicoRepository topicos = mock(TopicoRepository.class);
    private final ComentarioRepository comentarios = mock(ComentarioRepository.class);
    private final UsuarioForumService usuariosForum = mock(UsuarioForumService.class);
    private final PrivilegiosAdministrativosService privilegios = mock(PrivilegiosAdministrativosService.class);
    private final LogAuditoriaService auditoria = mock(LogAuditoriaService.class);
    private final DenunciaForumService servico = new DenunciaForumService(denuncias, topicos, comentarios,
            usuariosForum, privilegios, auditoria);
    private Topico topico;
    private Comentario comentario;

    @BeforeEach
    void preparar() {
        topico = new Topico();
        topico.setId(5L);
        topico.setIdAutor(2L);
        topico.setTitulo("Titulo");
        topico.setDescricao("x".repeat(400));
        topico.setNomeAutor("Autor A.");
        comentario = new Comentario();
        comentario.setId(9L);
        comentario.setIdAutor(2L);
        comentario.setConteudo("Comentario");
        comentario.setTopico(topico);
        when(topicos.findById(5L)).thenReturn(Optional.of(topico));
        when(comentarios.findById(9L)).thenReturn(Optional.of(comentario));
        when(usuariosForum.bloquearAtivo(1L)).thenReturn(new UsuarioForum(1L, "Denunciante", "d@x.com", "ALUNO"));
        when(denuncias.save(any(DenunciaForum.class))).thenAnswer(chamada -> {
            DenunciaForum denuncia = chamada.getArgument(0);
            ReflectionTestUtils.setField(denuncia, "id", 70L);
            return denuncia;
        });
    }

    @Test
    void registraDenunciaDeTopicoComDescricaoLimpa() {
        var resposta = servico.denunciar(TipoConteudoForum.TOPICO, 5L,
                new DenunciaRequest(MotivoDenuncia.OFENSA, "  texto agressivo  "), 1L);

        assertThat(resposta.id()).isEqualTo(70L);
        assertThat(resposta.status()).isEqualTo(StatusDenuncia.PENDENTE);
        verify(denuncias).save(argThat(d -> d.getIdAutorConteudo().equals(2L)
                && "texto agressivo".equals(d.getDescricao()) && d.getTipo() == TipoConteudoForum.TOPICO));
        verify(auditoria).registrar(eq(1L), eq("DENUNCIA_FORUM_REGISTRADA"), contains("OFENSA"), isNull());
    }

    @Test
    void denunciaDeComentarioSemDescricaoGuardaNulo() {
        servico.denunciar(TipoConteudoForum.COMENTARIO, 9L, new DenunciaRequest(MotivoDenuncia.SPAM, "   "), 1L);

        verify(denuncias).save(argThat(d -> d.getDescricao() == null && d.getTipo() == TipoConteudoForum.COMENTARIO));
    }

    @Test
    void naoPermiteDenunciarOProprioConteudoNemRepetirDenuncia() {
        when(usuariosForum.bloquearAtivo(2L)).thenReturn(new UsuarioForum(2L, "Autor", "a@x.com", "ALUNO"));
        assertThatThrownBy(() -> servico.denunciar(TipoConteudoForum.TOPICO, 5L,
                new DenunciaRequest(MotivoDenuncia.OFENSA, null), 2L)).isInstanceOf(RequisicaoInvalidaException.class);

        when(denuncias.existsByIdDenuncianteAndIdAutorConteudoAndCriadaEmAfter(eq(1L), eq(2L), any()))
                .thenReturn(true);
        assertThatThrownBy(() -> servico.denunciar(TipoConteudoForum.TOPICO, 5L,
                new DenunciaRequest(MotivoDenuncia.OFENSA, null), 1L))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("você já denunciou este usuário");
        verify(denuncias, never()).save(any());
    }

    @Test
    void conteudoInexistenteOuInativoNaoPodeSerDenunciado() {
        topico.setAtivo(false);
        assertThatThrownBy(() -> servico.denunciar(TipoConteudoForum.TOPICO, 5L,
                new DenunciaRequest(MotivoDenuncia.OFENSA, null), 1L)).isInstanceOf(RecursoNaoEncontradoException.class);
        comentario.setAtivo(false);
        assertThatThrownBy(() -> servico.denunciar(TipoConteudoForum.COMENTARIO, 9L,
                new DenunciaRequest(MotivoDenuncia.OFENSA, null), 1L)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> servico.denunciar(TipoConteudoForum.COMENTARIO, 99L,
                new DenunciaRequest(MotivoDenuncia.OFENSA, null), 1L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void listaPendentesPorPadraoSemExporDenuncianteEResumeOTrecho() {
        var denuncia = new DenunciaForum(TipoConteudoForum.TOPICO, 5L, 2L, 1L, MotivoDenuncia.OFENSA, null);
        when(denuncias.findByStatusOrderByCriadaEmAscIdAsc(StatusDenuncia.PENDENTE)).thenReturn(List.of(denuncia));
        when(denuncias.countByTipoAndIdConteudoAndStatus(TipoConteudoForum.TOPICO, 5L, StatusDenuncia.PENDENTE))
                .thenReturn(3L);

        var lista = servico.listar(10L, null);

        verify(privilegios).verificarAdministrador(10L);
        assertThat(lista).singleElement().satisfies(item -> {
            assertThat(item.trecho()).endsWith("...").hasSize(303);
            assertThat(item.conteudoAtivo()).isTrue();
            assertThat(item.nomeAutor()).isEqualTo("Autor A.");
            assertThat(item.denunciasPendentesDoConteudo()).isEqualTo(3);
            assertThat(item.idTopico()).isEqualTo(5L);
        });
    }

    @Test
    void listaComentarioMostraTopicoDeOrigem() {
        var denuncia = new DenunciaForum(TipoConteudoForum.COMENTARIO, 9L, 2L, 1L, MotivoDenuncia.SPAM, "spam");
        when(denuncias.findByStatusOrderByCriadaEmAscIdAsc(StatusDenuncia.IMPROCEDENTE)).thenReturn(List.of(denuncia));

        var lista = servico.listar(10L, StatusDenuncia.IMPROCEDENTE);

        assertThat(lista.getFirst().idTopico()).isEqualTo(5L);
        assertThat(lista.getFirst().trecho()).isEqualTo("Comentario");
    }

    @Test
    void decisaoProcedenteComRemocaoArquivaConteudoEEncerraDenunciasDoMesmoConteudo() {
        var denuncia = new DenunciaForum(TipoConteudoForum.COMENTARIO, 9L, 2L, 1L, MotivoDenuncia.OFENSA, null);
        var outra = new DenunciaForum(TipoConteudoForum.COMENTARIO, 9L, 2L, 3L, MotivoDenuncia.OFENSA, null);
        when(denuncias.findById(70L)).thenReturn(Optional.of(denuncia));
        when(denuncias.findByTipoAndIdConteudoAndStatus(TipoConteudoForum.COMENTARIO, 9L, StatusDenuncia.PENDENTE))
                .thenReturn(List.of(denuncia, outra));

        var resposta = servico.decidir(70L, new DecisaoDenunciaRequest(true, true, " Ofensa confirmada "), 10L);

        assertThat(comentario.isAtivo()).isFalse();
        verify(comentarios).save(comentario);
        assertThat(denuncia.getStatus()).isEqualTo(StatusDenuncia.PROCEDENTE);
        assertThat(outra.getStatus()).isEqualTo(StatusDenuncia.PROCEDENTE);
        assertThat(outra.isConteudoRemovido()).isTrue();
        assertThat(denuncia.getObservacaoModeracao()).isEqualTo("Ofensa confirmada");
        assertThat(resposta.conteudoAtivo()).isFalse();
    }

    @Test
    void decisaoComRemocaoDeTopicoArquivaOTopico() {
        var denuncia = new DenunciaForum(TipoConteudoForum.TOPICO, 5L, 2L, 1L, MotivoDenuncia.OFENSA, null);
        when(denuncias.findById(70L)).thenReturn(Optional.of(denuncia));
        when(denuncias.findByTipoAndIdConteudoAndStatus(TipoConteudoForum.TOPICO, 5L, StatusDenuncia.PENDENTE))
                .thenReturn(List.of(denuncia));

        servico.decidir(70L, new DecisaoDenunciaRequest(true, true, "Removido"), 10L);

        assertThat(topico.isAtivo()).isFalse();
        verify(topicos).save(topico);
    }

    @Test
    void decisaoImprocedenteMantemConteudoEDecisaoRepetidaERecusada() {
        var denuncia = new DenunciaForum(TipoConteudoForum.TOPICO, 5L, 2L, 1L, MotivoDenuncia.OFENSA, null);
        when(denuncias.findById(70L)).thenReturn(Optional.of(denuncia));

        servico.decidir(70L, new DecisaoDenunciaRequest(false, true, "Sem ofensa"), 10L);

        assertThat(denuncia.getStatus()).isEqualTo(StatusDenuncia.IMPROCEDENTE);
        assertThat(denuncia.isConteudoRemovido()).isFalse();
        assertThat(topico.isAtivo()).isTrue();
        verify(auditoria).registrar(eq(10L), eq("DENUNCIA_FORUM_DECIDIDA"), contains("improcedente"), isNull());
        assertThatThrownBy(() -> servico.decidir(70L, new DecisaoDenunciaRequest(true, false, "De novo"), 10L))
                .isInstanceOf(ConflitoException.class);
        when(denuncias.findById(71L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servico.decidir(71L, new DecisaoDenunciaRequest(true, false, "x"), 10L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
