package com.cursovalido.backend.aulas.service;

import com.cursovalido.backend.aulas.entity.ConclusaoCurso;
import com.cursovalido.backend.aulas.entity.GamificacaoAluno;
import com.cursovalido.backend.aulas.entity.Matricula;
import com.cursovalido.backend.aulas.repository.ConclusaoCursoRepository;
import com.cursovalido.backend.aulas.repository.CursoRepository;
import com.cursovalido.backend.aulas.repository.GamificacaoAlunoRepository;
import com.cursovalido.backend.aulas.repository.MatriculaRepository;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.Usuario;
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
    private final GamificacaoAlunoRepository gamificacoes = mock(GamificacaoAlunoRepository.class);
    private final ConclusaoCursoRepository conclusoes = mock(ConclusaoCursoRepository.class);
    private final MatriculaRepository matriculas = mock(MatriculaRepository.class);
    private final CursoRepository cursos = mock(CursoRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final LogAuditoriaService auditoria = mock(LogAuditoriaService.class);
    private final GamificacaoService servico = new GamificacaoService(gamificacoes, conclusoes, matriculas, cursos,
            usuarios, auditoria);
    private GamificacaoAluno registro;

    @BeforeEach
    void preparar() {
        registro = BeanUtils.instantiateClass(GamificacaoAluno.class);
        ReflectionTestUtils.setField(registro, "idAluno", 1L);
        when(cursos.findById(10L)).thenReturn(Optional.of(AulasTestSupport.curso()));
        when(gamificacoes.bloquear(1L)).thenReturn(Optional.of(registro));
    }

    @Test
    void xpDoCursoConsideraQuantidadeDeAulasComTeto() {
        assertThat(GamificacaoService.calcularXpCurso(0)).isEqualTo(100);
        assertThat(GamificacaoService.calcularXpCurso(5)).isEqualTo(150);
        assertThat(GamificacaoService.calcularXpCurso(1000)).isEqualTo(GamificacaoService.XP_MAXIMO_CURSO);
        assertThat(GamificacaoService.calcularXpCurso(-3)).isEqualTo(100);
    }

    @Test
    void cursoConcluidoConcedeXpUmaUnicaVezESobeNivel() {
        when(conclusoes.contarAulasAtivas(10L)).thenReturn(5L);
        when(conclusoes.contarAulasConcluidas(40L, 10L)).thenReturn(5L);
        when(conclusoes.countByIdAluno(1L)).thenReturn(1L);

        var recompensa = servico.registrarConclusao(1L, 10L, 40L);

        assertThat(recompensa.xpGanho()).isEqualTo(150);
        assertThat(recompensa.nivelAnterior()).isEqualTo(1);
        assertThat(recompensa.nivelAtual()).isEqualTo(2);
        assertThat(recompensa.subiuNivel()).isTrue();
        assertThat(recompensa.situacao().xpTotal()).isEqualTo(150);
        assertThat(recompensa.situacao().xpNivel()).isEqualTo(50);
        assertThat(recompensa.situacao().cursosConcluidos()).isEqualTo(1);
        verify(gamificacoes).criarSeAusente(1L);
        verify(gamificacoes).save(registro);
        verify(conclusoes).save(any(ConclusaoCurso.class));
        verify(auditoria).registrar(eq(1L), eq("CONCLUSAO_CURSO"), contains("XP 150"), isNull());
    }

    @Test
    void cursoJaRecompensadoNaoGeraNovoXp() {
        when(conclusoes.existsByIdAlunoAndIdCurso(1L, 10L)).thenReturn(true);

        assertThat(servico.registrarConclusao(1L, 10L, 40L)).isNull();
        verify(gamificacoes, never()).save(any());
    }

    @Test
    void conclusaoConcorrenteDetectadaAposBloqueioNaoDuplicaXp() {
        when(conclusoes.contarAulasAtivas(10L)).thenReturn(2L);
        when(conclusoes.contarAulasConcluidas(40L, 10L)).thenReturn(2L);
        when(conclusoes.existsByIdAlunoAndIdCurso(1L, 10L)).thenReturn(false, true);

        assertThat(servico.registrarConclusao(1L, 10L, 40L)).isNull();
        verify(gamificacoes, never()).save(any());
        verify(conclusoes, never()).save(any());
    }

    @Test
    void cursoIncompletoInativoOuSemAulasNaoRendeXp() {
        when(conclusoes.contarAulasAtivas(10L)).thenReturn(4L);
        when(conclusoes.contarAulasConcluidas(40L, 10L)).thenReturn(3L);
        assertThat(servico.registrarConclusao(1L, 10L, 40L)).isNull();

        when(conclusoes.contarAulasAtivas(10L)).thenReturn(0L);
        assertThat(servico.registrarConclusao(1L, 10L, 40L)).isNull();

        var inativo = AulasTestSupport.curso();
        inativo.setAtivo(false);
        when(cursos.findById(11L)).thenReturn(Optional.of(inativo));
        assertThat(servico.registrarConclusao(1L, 11L, 41L)).isNull();
        verify(gamificacoes, never()).save(any());
    }

    @Test
    void resumoSemRegistroComecaNoNivelUm() {
        when(gamificacoes.findById(1L)).thenReturn(Optional.empty());

        var resumo = servico.resumo(1L);

        assertThat(resumo.nivel()).isEqualTo(1);
        assertThat(resumo.xpTotal()).isZero();
        assertThat(resumo.metaNivel()).isEqualTo(100);
        assertThat(resumo.xpParaProximoNivel()).isEqualTo(100);
        assertThat(resumo.percentualNivel()).isZero();
        assertThat(resumo.nivelMaximo()).isFalse();
    }

    @Test
    void resumoNoNivelMaximoMostraBarraCompleta() {
        registro.atualizar(ProgressaoNivel.NIVEL_MAXIMO, 10, 99_999);
        when(gamificacoes.findById(1L)).thenReturn(Optional.of(registro));

        var resumo = servico.resumo(1L);

        assertThat(resumo.nivelMaximo()).isTrue();
        assertThat(resumo.percentualNivel()).isEqualTo(100);
        assertThat(resumo.xpParaProximoNivel()).isZero();
    }

    @Test
    void consultarAtualizaMatriculasDoAlunoAtivo() {
        Usuario aluno = new Usuario();
        aluno.setPerfil(Perfil.ALUNO);
        aluno.setAtivo(true);
        when(usuarios.findById(1L)).thenReturn(Optional.of(aluno));
        Matricula matricula = AulasTestSupport.matricula();
        when(matriculas.findByIdAlunoOrderByIdDesc(1L)).thenReturn(List.of(matricula));
        when(gamificacoes.findById(1L)).thenReturn(Optional.empty());

        assertThat(servico.consultar(1L).nivel()).isEqualTo(1);
        verify(conclusoes).existsByIdAlunoAndIdCurso(1L, 10L);
    }

    @Test
    void consultarRecusaQuemNaoEAlunoAtivo() {
        Usuario professor = new Usuario();
        professor.setPerfil(Perfil.PROFESSOR);
        professor.setAtivo(true);
        when(usuarios.findById(2L)).thenReturn(Optional.of(professor));
        when(usuarios.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.consultar(2L)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> servico.consultar(3L)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void listaMatriculasPendentesDoRepositorio() {
        when(conclusoes.matriculasSemConclusao()).thenReturn(List.of(AulasTestSupport.matricula()));
        assertThat(servico.matriculasPendentesDeConclusao()).hasSize(1);
    }
}
