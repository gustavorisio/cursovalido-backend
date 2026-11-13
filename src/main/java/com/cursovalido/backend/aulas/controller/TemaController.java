package com.cursovalido.backend.aulas.controller;

import com.cursovalido.backend.aulas.dto.TemaRequest;
import com.cursovalido.backend.aulas.entity.Tema;
import com.cursovalido.backend.aulas.service.TemaService;
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
public class TemaController {
    private final TemaService servico;

    @GetMapping("/catalogo/temas")
    public List<Tema> listar() {
        return servico.listar();
    }

    @PostMapping("/temas")
    @ResponseStatus(HttpStatus.CREATED)
    public Tema criar(@Valid @RequestBody TemaRequest dados, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.salvar(null, dados.nome(), usuario.getId());
    }

    @PutMapping("/temas/{id}")
    public Tema editar(@PathVariable Long id, @Valid @RequestBody TemaRequest dados, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.salvar(id, dados.nome(), usuario.getId());
    }

    @DeleteMapping("/temas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        servico.excluir(id, usuario.getId());
    }
}
