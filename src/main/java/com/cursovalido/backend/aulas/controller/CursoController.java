package com.cursovalido.backend.aulas.controller;

import com.cursovalido.backend.aulas.dto.CursoRequest;
import com.cursovalido.backend.aulas.dto.CursoResponse;
import com.cursovalido.backend.aulas.entity.Curso;
import com.cursovalido.backend.aulas.service.CursoService;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CursoController {
    private final CursoService servico;

    public record CursoPublico(Long id, String titulo, String descricao, Long idTema, String nomeTema, Long idArea) {
    }

    @GetMapping("/catalogo/cursos")
    public List<CursoPublico> catalogoPublico() {
        return servico.listarPublicos().stream()
                .map(curso -> new CursoPublico(curso.getId(), curso.getTitulo(), curso.getDescricao(), curso.getTema() == null ? null : curso.getTema().getId(), curso.getTema() == null ? null : curso.getTema().getNome(), curso.getArea() == null ? null : curso.getArea().getId())).toList();
    }

    @GetMapping("/cursos")
    public List<CursoResponse> listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.listar(usuario.getId()).stream().map(this::converter).toList();
    }

    @GetMapping("/cursos/{id}")
    public CursoResponse buscar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return converter(servico.buscarAtivo(id, usuario.getId()));
    }

    @PostMapping("/cursos")
    @ResponseStatus(HttpStatus.CREATED)
    public CursoResponse criar(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody CursoRequest dados) {
        return converter(servico.criar(dados, usuario.getId()));
    }

    @PutMapping("/cursos/{id}")
    public CursoResponse editar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody CursoRequest dados) {
        return converter(servico.editar(id, dados, usuario.getId()));
    }

    @DeleteMapping("/cursos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        servico.apagar(id, usuario.getId());
    }

    private CursoResponse converter(Curso item) {
        return CursoResponse.de(item);
    }
}
