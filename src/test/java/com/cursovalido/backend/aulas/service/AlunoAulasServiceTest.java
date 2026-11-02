package com.cursovalido.backend.aulas.service;

import com.cursovalido.backend.aulas.repository.*;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.repository.UsuarioRepositorio;
import com.cursovalido.backend.authentication.service.LogAuditoriaServico;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.cursovalido.backend.aulas.service.AulasTestSupport.*;

@ExtendWith(MockitoExtension.class)
class AlunoAulasServiceTest {
    @Mock private UsuarioRepositorio usuarios;
    @Mock private CursoRepository cursos;
    @Mock private ModuloRepository modulos;
    @Mock private AulaRepository aulas;
    @Mock private MatriculaRepository matriculas;
    @Mock private LogAuditoriaServico auditoria;
    @InjectMocks private AlunoAulasService servico;

    private void autorizar() {
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario(Perfil.ALUNO)));
    }

    private void matriculado() {
        autorizar();
        when(cursos.findById(10L)).thenReturn(Optional.of(curso()));
        when(matriculas.findByIdAlunoAndIdCurso(1L, 10L)).thenReturn(Optional.of(matricula()));
    }

    @Test
    void catalogoNaoExigeMatricula() {
        autorizar();
        var curso = curso();
        when(cursos.findByAtivoTrueOrderByIdDesc()).thenReturn(List.of(curso));
        assertThat(servico.listarCursos(1L)).containsExactly(curso);
        verifyNoInteractions(matriculas, aulas);
    }

    @Test
    void listarMatriculasDoProprioAluno() {
        autorizar();
        var matricula = matricula();
        when(matriculas.findByIdAlunoOrderByIdDesc(1L)).thenReturn(List.of(matricula));
        assertThat(servico.listarMatriculas(1L)).containsExactly(matricula);
    }

    @Test
    void criarMatriculaGratuitaComDataEAuditoria() {
        autorizar();
        when(cursos.buscarParaMatricula(10L)).thenReturn(Optional.of(curso()));
        var salvo = servico.matricular(10L, 1L);
        assertThat(salvo.getIdAluno()).isEqualTo(1L);
        assertThat(salvo.getIdCurso()).isEqualTo(10L);
        assertThat(salvo.getMatriculadoEm()).isNotNull();
        verify(matriculas).save(salvo);
        verify(auditoria).registrar(1L, "MATRICULA_CURSO", "Curso 10", null);
    }

    @Test
    void repetirMatriculaNaoSalvaNemAuditaNovamente() {
        autorizar();
        when(cursos.buscarParaMatricula(10L)).thenReturn(Optional.of(curso()));
        var matricula = matricula();
        when(matriculas.findByIdAlunoAndIdCurso(1L, 10L)).thenReturn(Optional.of(matricula));
        assertThat(servico.matricular(10L, 1L)).isSameAs(matricula);
        verify(matriculas, never()).save(any());
        verifyNoInteractions(auditoria);
    }

    @Test
    void matriculaEmCursoAusenteRetorna404() {
        autorizar();
        erro(() -> servico.matricular(10L, 1L), 404);
        verifyNoInteractions(matriculas);
    }

    @Test
    void matriculaEmCursoInativoRetorna404() {
        autorizar();
        var curso = curso();
        curso.setAtivo(false);
        when(cursos.buscarParaMatricula(10L)).thenReturn(Optional.of(curso));
        erro(() -> servico.matricular(10L, 1L), 404);
        verifyNoInteractions(matriculas);
    }

    @Test
    void consultarCursoAusenteRetorna404() {
        autorizar();
        erro(() -> servico.buscarCurso(10L, 1L), 404);
    }

    @Test
    void alunoSemMatriculaNaoConsultaConteudo() {
        autorizar();
        when(cursos.findById(10L)).thenReturn(Optional.of(curso()));
        erro(() -> servico.buscarCurso(10L, 1L), 403);
    }

    @Test
    void listarModulosExigeMatriculaNoCurso() {
        matriculado();
        var modulo = modulo();
        when(modulos.findByIdCursoAndAtivoTrueOrderByOrdemAscIdAsc(10L)).thenReturn(List.of(modulo));
        assertThat(servico.listarModulos(10L, 1L)).containsExactly(modulo);
    }

    @Test
    void listarAulasUsaCursoRealDoModulo() {
        matriculado();
        when(modulos.findById(20L)).thenReturn(Optional.of(modulo()));
        var aula = aula();
        when(aulas.findByIdModuloAndAtivoTrueOrderByOrdemAscIdAsc(20L)).thenReturn(List.of(aula));
        assertThat(servico.listarAulas(20L, 1L)).containsExactly(aula);
    }

    @Test
    void buscarAulaVerificaTodaHierarquia() {
        matriculado();
        when(modulos.findById(20L)).thenReturn(Optional.of(modulo()));
        var aula = aula();
        when(aulas.findById(30L)).thenReturn(Optional.of(aula));
        assertThat(servico.buscarAula(30L, 1L)).isSameAs(aula);
        verify(matriculas).findByIdAlunoAndIdCurso(1L, 10L);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void moduloAusenteOuInativoRetorna404(boolean existente) {
        autorizar();
        if (existente) {
            var modulo = modulo();
            modulo.setAtivo(false);
            when(modulos.findById(20L)).thenReturn(Optional.of(modulo));
        }
        erro(() -> servico.buscarModulo(20L, 1L), 404);
        verifyNoInteractions(cursos, matriculas);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void aulaAusenteOuInativaRetorna404(boolean existente) {
        autorizar();
        if (existente) {
            var aula = aula();
            aula.setAtivo(false);
            when(aulas.findById(30L)).thenReturn(Optional.of(aula));
        }
        erro(() -> servico.buscarAula(30L, 1L), 404);
        verifyNoInteractions(modulos, cursos, matriculas);
    }

    @Test
    void usuarioAusenteNaoConsultaCursos() {
        erro(() -> servico.listarCursos(1L), 403);
        verifyNoInteractions(cursos);
    }

    @ParameterizedTest
    @ValueSource(strings = { "inativo", "professor", "administrador", "email", "termos", "termos-nulos" })
    void rejeitarAlunoSemRequisitosDeAcesso(String motivo) {
        var aluno = usuario(Perfil.ALUNO);
        switch (motivo) {
            case "inativo" -> aluno.setAtivo(false);
            case "professor" -> aluno.setPerfil(Perfil.PROFESSOR);
            case "administrador" -> aluno.setPerfil(Perfil.ADMINISTRADOR);
            case "email" -> aluno.setEmailConfirmado(false);
            case "termos" -> aluno.setAceitouTermosLgpd(false);
            case "termos-nulos" -> aluno.setAceitouTermosLgpd(null);
        }
        when(usuarios.findById(1L)).thenReturn(Optional.of(aluno));
        erro(() -> servico.listarCursos(1L), 403);
        verifyNoInteractions(cursos, matriculas);
    }
}
