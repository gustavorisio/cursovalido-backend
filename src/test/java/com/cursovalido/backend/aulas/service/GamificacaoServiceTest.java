package com.cursovalido.backend.aulas.service;

import com.cursovalido.backend.aulas.entity.*;
import com.cursovalido.backend.aulas.repository.*;
import com.cursovalido.backend.authentication.entity.*;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GamificacaoServiceTest {
    private final RecompensaQuestionarioRepository recompensas = mock(RecompensaQuestionarioRepository.class);
    private final GamificacaoAlunoRepository gamificacoes = mock(GamificacaoAlunoRepository.class);
    private final ConclusaoCursoRepository conclusoes = mock(ConclusaoCursoRepository.class);
    private final CursoRepository cursos = mock(CursoRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final LogAuditoriaService auditoria = mock(LogAuditoriaService.class);
    private final GamificacaoService servico = new GamificacaoService(recompensas, gamificacoes, conclusoes, cursos, usuarios, auditoria);
    private GamificacaoAluno registro;
    private Questionario questionario;

    @BeforeEach
    void preparar() {
        registro = BeanUtils.instantiateClass(GamificacaoAluno.class);
        ReflectionTestUtils.setField(registro, "idAluno", 1L);
        when(gamificacoes.bloquear(1L)).thenReturn(Optional.of(registro));
        when(gamificacoes.findById(1L)).thenReturn(Optional.of(registro));
        Usuario aluno = new Usuario();
        aluno.setPerfil(Perfil.ALUNO);
        aluno.setAtivo(true);
        when(usuarios.findById(1L)).thenReturn(Optional.of(aluno));
        when(usuarios.buscarParaAtualizar(1L)).thenReturn(Optional.of(aluno));
        questionario = new Questionario();
        questionario.setId(5L);
        questionario.setIdModulo(20L);
        questionario.setPublicado(true);
        questionario.setPerguntas(List.of(new Questao(), new Questao()));
    }

    @Test
    void cemPorCentoConcedeCemXpESobeNivel() {
        when(recompensas.countByIdAluno(1L)).thenReturn(1L);
        var recompensa = servico.registrarAprovacao(1L, questionario, 2);
        assertThat(recompensa.xpGanho()).isEqualTo(100);
        assertThat(recompensa.nivelAnterior()).isEqualTo(1);
        assertThat(recompensa.nivelAtual()).isEqualTo(2);
        assertThat(recompensa.subiuNivel()).isTrue();
        assertThat(recompensa.situacao().xpTotal()).isEqualTo(100);
        assertThat(recompensa.situacao().xpNivel()).isZero();
        assertThat(recompensa.situacao().questionariosConcluidos()).isEqualTo(1);
        verify(recompensas).saveAndFlush(any(RecompensaQuestionario.class));
        verify(auditoria).registrar(eq(1L), eq("APROVACAO_QUESTIONARIO"), contains("XP 100"), isNull());
    }

    @Test
    void resultadoParcialOuInvalidoNaoConcedeXp() {
        for (int acertos : List.of(0, 1, 3)) assertThat(servico.registrarAprovacao(1L, questionario, acertos)).isNull();
        verifyNoInteractions(recompensas, gamificacoes);
    }

    @Test
    void apenasQuestionarioPublicadoDoModuloComPerguntasPodeConcederXp() {
        questionario.setPublicado(false);
        assertThat(servico.registrarAprovacao(1L, questionario, 2)).isNull();
        questionario.setPublicado(true);
        questionario.setIdModulo(null);
        assertThat(servico.registrarAprovacao(1L, questionario, 2)).isNull();
        questionario.setIdModulo(20L);
        questionario.setPerguntas(List.of());
        assertThat(servico.registrarAprovacao(1L, questionario, 0)).isNull();
        verifyNoInteractions(recompensas, gamificacoes);
    }

    @Test
    void repeticaoMesmoEmNovaVersaoNaoDuplicaCredito() {
        when(recompensas.existsByIdAlunoAndIdQuestionario(1L, 5L)).thenReturn(true);
        assertThat(servico.registrarAprovacao(1L, questionario, 2)).isNull();
        questionario.setVersao(2);
        assertThat(servico.registrarAprovacao(1L, questionario, 2)).isNull();
        verify(usuarios, times(2)).buscarParaAtualizar(1L);
        verifyNoInteractions(gamificacoes);
        verify(recompensas, never()).saveAndFlush(any());
    }

    @Test
    void consultaPreservaSaldoAntigoSemConcederXp() {
        registro.atualizar(2, 50, 150);
        assertThat(servico.consultar(1L).xpTotal()).isEqualTo(150);
        verify(gamificacoes, never()).save(any());
        verifyNoInteractions(cursos);
    }

    @Test
    void aprovacaoAcrescentaXpAoSaldoExistente() {
        registro.atualizar(2, 50, 150);
        assertThat(servico.registrarAprovacao(1L, questionario, 2).situacao().xpTotal()).isEqualTo(250);
        assertThat(registro.getNivel()).isEqualTo(3);
    }

    @Test
    void conclusaoDoCursoMantemMetricaSemXp() {
        when(cursos.findById(10L)).thenReturn(Optional.of(AulasTestSupport.curso()));
        when(conclusoes.contarAulasAtivas(10L)).thenReturn(1L);
        when(conclusoes.contarAulasConcluidas(40L, 10L)).thenReturn(1L);
        assertThat(servico.registrarConclusao(1L, 10L, 40L)).isTrue();
        verify(conclusoes).save(argThat(conclusao -> conclusao.getXpConcedido() == 0));
        verifyNoInteractions(gamificacoes, recompensas);
    }

    @Test
    void consultaExigeAlunoAtivo() {
        Usuario professor = new Usuario();
        professor.setPerfil(Perfil.PROFESSOR);
        professor.setAtivo(true);
        when(usuarios.findById(2L)).thenReturn(Optional.of(professor));
        assertThatThrownBy(() -> servico.consultar(2L)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> servico.consultar(3L)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void resumoSemRegistroComecaNoNivelUm() {
        when(gamificacoes.findById(1L)).thenReturn(Optional.empty());
        var resumo = servico.resumo(1L);
        assertThat(resumo.nivel()).isEqualTo(1);
        assertThat(resumo.xpTotal()).isZero();
        assertThat(resumo.metaNivel()).isEqualTo(100);
    }

    @Test
    void resumoNoNivelMaximoMostraBarraCompleta() {
        registro.atualizar(ProgressaoNivel.NIVEL_MAXIMO, 10, 99_999);
        assertThat(servico.resumo(1L).percentualNivel()).isEqualTo(100);
        assertThat(servico.resumo(1L).xpParaProximoNivel()).isZero();
    }
}
