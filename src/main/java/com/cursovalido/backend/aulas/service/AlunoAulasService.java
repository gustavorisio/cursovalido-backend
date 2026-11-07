package com.cursovalido.backend.aulas.service;

import com.cursovalido.backend.aulas.entity.*;
import com.cursovalido.backend.aulas.repository.*;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AlunoAulasService {
    private final UsuarioRepository usuarios;
    private final CursoRepository cursos;
    private final ModuloRepository modulos;
    private final AulaRepository aulas;
    private final MatriculaRepository matriculas;
    private final LogAuditoriaService auditoria;

    // O catalogo mostra somente os dados dos cursos, sem os videos.
    public List<Curso> listarCursos(Long idAluno) {
        verificarAluno(idAluno);
        return cursos.findByAtivoTrueOrderByIdDesc();
    }

    public List<Matricula> listarMatriculas(Long idAluno) {
        verificarAluno(idAluno);
        return matriculas.findByIdAlunoOrderByIdDesc(idAluno);
    }

    @Transactional
    public Matricula matricular(Long idCurso, Long idAluno) {
        verificarAluno(idAluno);
        // O bloqueio aguarda outra matricula no mesmo curso terminar antes de conferir duplicidade.
        Curso curso = cursos.buscarParaMatricula(idCurso)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso nao encontrado"));
        verificarCursoAtivo(curso);
        var existente = matriculas.findByIdAlunoAndIdCurso(idAluno, idCurso);
        if (existente.isPresent()) {
            return existente.get();
        }
        Matricula matricula = new Matricula();
        matricula.setIdAluno(idAluno);
        matricula.setIdCurso(idCurso);
        matricula.setMatriculadoEm(LocalDateTime.now());
        matriculas.save(matricula);
        auditoria.registrar(idAluno, "MATRICULA_CURSO", "Curso " + idCurso, null);
        return matricula;
    }

    public Curso buscarCurso(Long idCurso, Long idAluno) {
        verificarAluno(idAluno);
        Curso curso = cursos.findById(idCurso)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso nao encontrado"));
        verificarCursoAtivo(curso);
        if (matriculas.findByIdAlunoAndIdCurso(idAluno, idCurso).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Matricule-se no curso para acessar as aulas");
        }
        return curso;
    }

    public List<Modulo> listarModulos(Long idCurso, Long idAluno) {
        buscarCurso(idCurso, idAluno);
        return modulos.findByIdCursoAndAtivoTrueOrderByOrdemAscIdAsc(idCurso);
    }

    public Modulo buscarModulo(Long idModulo, Long idAluno) {
        verificarAluno(idAluno);
        Modulo modulo = modulos.findById(idModulo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modulo nao encontrado"));
        if (!modulo.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Modulo inativo");
        }
        buscarCurso(modulo.getIdCurso(), idAluno);
        return modulo;
    }

    public List<Aula> listarAulas(Long idModulo, Long idAluno) {
        buscarModulo(idModulo, idAluno);
        return aulas.findByIdModuloAndAtivoTrueOrderByOrdemAscIdAsc(idModulo);
    }

    public Aula buscarAula(Long idAula, Long idAluno) {
        verificarAluno(idAluno);
        Aula aula = aulas.findById(idAula)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aula nao encontrada"));
        if (!aula.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aula inativa");
        }
        buscarModulo(aula.getIdModulo(), idAluno);
        return aula;
    }

    private void verificarCursoAtivo(Curso curso) {
        if (!curso.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso inativo");
        }
    }

    private void verificarAluno(Long idAluno) {
        var aluno = usuarios.findById(idAluno)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Aluno nao encontrado"));
        if (!aluno.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Aluno inativo");
        }
        if (aluno.getPerfil() != Perfil.ALUNO) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso exclusivo de aluno");
        }
        if (!aluno.isEmailConfirmado()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Confirme o email para acessar as aulas");
        }
        if (!Boolean.TRUE.equals(aluno.getAceitouTermosLgpd())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Aceite os termos para acessar as aulas");
        }
    }
}
