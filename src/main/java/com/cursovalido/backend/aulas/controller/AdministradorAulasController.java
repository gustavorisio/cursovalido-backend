package com.cursovalido.backend.aulas.controller;

import com.cursovalido.backend.aulas.dto.*;
import com.cursovalido.backend.aulas.entity.*;
import com.cursovalido.backend.aulas.service.AdministradorAulasService;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/administracao")
@RequiredArgsConstructor
public class AdministradorAulasController {
    private final AdministradorAulasService servico;

    @GetMapping("/cursos")
    public List<CursoResponse> listarCursos(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.listarCursos(usuario.getId()).stream().map(this::converter).toList();
    }

    @GetMapping("/cursos/{id}")
    public CursoResponse buscarCurso(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return converter(servico.buscarCurso(id, usuario.getId()));
    }

    @GetMapping("/cursos/{idCurso}/modulos")
    public List<ModuloResponse> listarModulos(@PathVariable Long idCurso, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.listarModulos(idCurso, usuario.getId()).stream().map(this::converter).toList();
    }

    @GetMapping("/modulos/{id}/aulas")
    public List<AulaResponse> listarAulas(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.listarAulas(id, usuario.getId()).stream().map(this::converter).toList();
    }

    @GetMapping("/aulas/{id}")
    public AulaResponse buscarAula(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return converter(servico.buscarAula(id, usuario.getId()));
    }

    private CursoResponse converter(Curso curso) {
        return new CursoResponse(curso.getId(), curso.getTitulo(), curso.getDescricao(), curso.getIdProfessor());
    }

    private ModuloResponse converter(Modulo modulo) {
        return new ModuloResponse(modulo.getId(), modulo.getTitulo(), modulo.getDescricao(), modulo.getIdCurso(), modulo.getOrdem());
    }

    private AulaResponse converter(Aula aula) {
        return new AulaResponse(aula.getId(), aula.getTitulo(), aula.getDescricao(), aula.getIdModulo(), aula.getOrdem(), aula.getYoutubeVideoId());
    }
}
