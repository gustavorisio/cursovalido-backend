package com.cursovalido.backend.aulas.controller;

import com.cursovalido.backend.aulas.dto.ModuloRequest;
import com.cursovalido.backend.aulas.dto.ModuloResponse;
import com.cursovalido.backend.aulas.entity.Modulo;
import com.cursovalido.backend.aulas.service.ModuloService;
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
public class ModuloController {
    private final ModuloService servico;

    @GetMapping("/cursos/{idCurso}/modulos")
    public List<ModuloResponse> listar(@PathVariable Long idCurso, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.listar(idCurso, usuario.getId()).stream().map(this::converter).toList();
    }

    @GetMapping("/modulos/{id}")
    public ModuloResponse buscar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return converter(servico.buscarAtivo(id, usuario.getId()));
    }

    @PostMapping("/cursos/{idCurso}/modulos")
    @ResponseStatus(HttpStatus.CREATED)
    public ModuloResponse criar(@PathVariable Long idCurso, @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ModuloRequest dados) {
        return converter(servico.criar(idCurso, dados, usuario.getId()));
    }

    @PutMapping("/modulos/{id}")
    public ModuloResponse editar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ModuloRequest dados) {
        return converter(servico.editar(id, dados, usuario.getId()));
    }

    @DeleteMapping("/modulos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        servico.apagar(id, usuario.getId());
    }

    private ModuloResponse converter(Modulo item) {
        return new ModuloResponse(item.getId(), item.getTitulo(), item.getDescricao(), item.getIdCurso(), item.getOrdem());
    }
}
