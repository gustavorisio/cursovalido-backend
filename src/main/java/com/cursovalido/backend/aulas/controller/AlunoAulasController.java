package com.cursovalido.backend.aulas.controller;

import com.cursovalido.backend.aulas.dto.*;
import com.cursovalido.backend.aulas.entity.*;
import com.cursovalido.backend.aulas.service.AlunoAulasService;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/aluno")
@RequiredArgsConstructor
public class AlunoAulasController {
    private final AlunoAulasService servico;

    @GetMapping("/cursos")
    public List<CursoResponse> listarCursos(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.listarCursos(usuario.getId()).stream().map(this::converter).toList();
    }

    // Repetir a chamada devolve a mesma matricula, sem cobrar ou criar outro
    // registro.
    @PostMapping("/cursos/{idCurso}/matriculas")
    public MatriculaResponse matricular(@PathVariable Long idCurso,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return converter(servico.matricular(idCurso, usuario.getId()));
    }

    @GetMapping("/matriculas")
    public List<MatriculaResponse> listarMatriculas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.listarMatriculas(usuario.getId()).stream().map(this::converter).toList();
    }

    @GetMapping("/cursos/{idCurso}")
    public CursoResponse buscarCurso(@PathVariable Long idCurso,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return converter(servico.buscarCurso(idCurso, usuario.getId()));
    }

    @GetMapping("/cursos/{idCurso}/modulos")
    public List<ModuloResponse> listarModulos(@PathVariable Long idCurso,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.listarModulos(idCurso, usuario.getId()).stream().map(this::converter).toList();
    }

    @GetMapping("/modulos/{idModulo}/aulas")
    public List<AulaResponse> listarAulas(@PathVariable Long idModulo,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.listarAulas(idModulo, usuario.getId()).stream().map(this::converter).toList();
    }

    @GetMapping("/aulas/{idAula}")
    public AulaResponse buscarAula(@PathVariable Long idAula,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return converter(servico.buscarAula(idAula, usuario.getId()));
    }

    private CursoResponse converter(Curso curso) {
        return CursoResponse.de(curso);
    }

    private ModuloResponse converter(Modulo modulo) {
        return new ModuloResponse(modulo.getId(), modulo.getTitulo(), modulo.getDescricao(),
                modulo.getIdCurso(), modulo.getOrdem());
    }

    private AulaResponse converter(Aula aula) {
        return new AulaResponse(aula.getId(), aula.getTitulo(), aula.getDescricao(),
                aula.getIdModulo(), aula.getOrdem(), aula.getYoutubeVideoId());
    }

    private MatriculaResponse converter(Matricula matricula) {
        return new MatriculaResponse(matricula.getId(), matricula.getIdCurso(), matricula.getMatriculadoEm());
    }
}
