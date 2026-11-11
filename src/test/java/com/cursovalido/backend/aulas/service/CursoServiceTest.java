package com.cursovalido.backend.aulas.service;

import com.cursovalido.backend.aulas.dto.CursoRequest;
import com.cursovalido.backend.aulas.repository.CursoRepository;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.cursovalido.backend.aulas.service.AulasTestSupport.*;

@ExtendWith(MockitoExtension.class)
class CursoServiceTest {
    @Mock
    private CursoRepository repositorio;
    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private LogAuditoriaService auditoria;
    @InjectMocks
    private CursoService servico;

    private void autorizar() {
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario(Perfil.PROFESSOR)));
    }

    @Test
    void criarAssociaProfessorNormalizaCamposEAudita() {
        autorizar();
        var salvo = servico.criar(new CursoRequest(" Java ", " Basico "), 1L);
        assertThat(salvo.getTitulo()).isEqualTo("Java");
        assertThat(salvo.getDescricao()).isEqualTo("Basico");
        assertThat(salvo.getIdProfessor()).isEqualTo(1L);
        assertThat(salvo.getCriadoEm()).isNotNull();
        assertThat(salvo.isAtivo()).isTrue();
        verify(repositorio).save(salvo);
        verify(auditoria).registrar(eq(1L), eq("CADASTRO_CURSO"), anyString(), isNull());
    }

    @Test
    void listarSomenteCursosDoProfessor() {
        autorizar();
        var curso = curso();
        when(repositorio.findByIdProfessorAndAtivoTrueOrderByIdDesc(1L)).thenReturn(List.of(curso));
        assertThat(servico.listar(1L)).containsExactly(curso);
    }

    @Test
    void editarPreservaResponsavelERegistraAuditoria() {
        autorizar();
        var curso = curso();
        when(repositorio.findById(10L)).thenReturn(Optional.of(curso));
        assertThat(servico.editar(10L, new CursoRequest(" Novo ", " Texto "), 1L)).isSameAs(curso);
        assertThat(curso.getTitulo()).isEqualTo("Novo");
        assertThat(curso.getDescricao()).isEqualTo("Texto");
        assertThat(curso.getIdProfessor()).isEqualTo(1L);
        verify(auditoria).registrar(1L, "EDICAO_CURSO", "Curso 10", null);
    }

    @Test
    void excluirInativaSemRemoverRegistro() {
        autorizar();
        var curso = curso();
        when(repositorio.findById(10L)).thenReturn(Optional.of(curso));
        servico.apagar(10L, 1L);
        assertThat(curso.isAtivo()).isFalse();
        verify(repositorio).save(curso);
        verify(repositorio, never()).delete(any());
        verify(auditoria).registrar(1L, "EXCLUSAO_CURSO", "Curso 10", null);
    }

    @Test
    void cursoAusenteRetorna404() {
        autorizar();
        erro(() -> servico.buscarAtivo(10L, 1L), 404);
    }

    @Test
    void cursoInativoRetorna404() {
        autorizar();
        var curso = curso();
        curso.setAtivo(false);
        when(repositorio.findById(10L)).thenReturn(Optional.of(curso));
        erro(() -> servico.buscarAtivo(10L, 1L), 404);
    }

    @Test
    void outroProfessorNaoPodeAlterar() {
        autorizar();
        var curso = curso();
        curso.setIdProfessor(2L);
        when(repositorio.findById(10L)).thenReturn(Optional.of(curso));
        erro(() -> servico.editar(10L, new CursoRequest("Novo", "Texto"), 1L), 403);
        verify(repositorio, never()).save(any());
        verifyNoInteractions(auditoria);
    }

    @Test
    void professorAusenteNaoConsultaRepositorioDeCursos() {
        erro(() -> servico.listar(1L), 403);
        verifyNoInteractions(repositorio);
    }

    @Test
    void professorInativoNaoCriaCurso() {
        var professor = usuario(Perfil.PROFESSOR);
        professor.setAtivo(false);
        when(usuarios.findById(1L)).thenReturn(Optional.of(professor));
        erro(() -> servico.criar(new CursoRequest("Java", "Basico"), 1L), 403);
        verifyNoInteractions(repositorio, auditoria);
    }

    @ParameterizedTest
    @EnumSource(value = Perfil.class, names = { "ALUNO", "ADMINISTRADOR" })
    void perfilIncorretoNaoGerenciaCurso(Perfil perfil) {
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario(perfil)));
        erro(() -> servico.listar(1L), 403);
        verifyNoInteractions(repositorio);
    }
}
