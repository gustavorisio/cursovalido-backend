package com.cursovalido.backend.aulas.controller;

import com.cursovalido.backend.aulas.dto.AulaRequest;
import com.cursovalido.backend.aulas.dto.AulaResponse;
import com.cursovalido.backend.aulas.entity.Aula;
import com.cursovalido.backend.aulas.service.AulaService;
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
public class AulaController {
    private final AulaService servico;

    @GetMapping("/modulos/{idModulo}/aulas")
    public List<AulaResponse> listar(@PathVariable Long idModulo, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.listar(idModulo, usuario.getId()).stream().map(this::converter).toList();
    }

    @GetMapping("/aulas/{id}")
    public AulaResponse buscar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return converter(servico.buscarAtivo(id, usuario.getId()));
    }

    @PostMapping("/modulos/{idModulo}/aulas")
    @ResponseStatus(HttpStatus.CREATED)
    public AulaResponse criar(@PathVariable Long idModulo, @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody AulaRequest dados) {
        return converter(servico.criar(idModulo, dados, usuario.getId()));
    }

    @PutMapping("/aulas/{id}")
    public AulaResponse editar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody AulaRequest dados) {
        return converter(servico.editar(id, dados, usuario.getId()));
    }

    @DeleteMapping("/aulas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        servico.apagar(id, usuario.getId());
    }

    private AulaResponse converter(Aula item) {
        return new AulaResponse(item.getId(), item.getTitulo(), item.getDescricao(), item.getIdModulo(), item.getOrdem(), item.getYoutubeVideoId());
    }
}
