package com.cursovalido.backend.noticias.controller;

import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import com.cursovalido.backend.noticias.dto.NoticiaRequest;
import com.cursovalido.backend.noticias.dto.NoticiaResponse;
import com.cursovalido.backend.noticias.entity.Noticia;
import com.cursovalido.backend.noticias.service.NoticiaService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/noticias")
@RequiredArgsConstructor
public class NoticiaController {
    private final NoticiaService servico;

    @GetMapping
    public List<NoticiaResponse> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "") String busca) {
        return servico.listarRecentes(page, size, busca).stream().map(this::converter).toList();
    }

    @GetMapping("/{id}")
    public NoticiaResponse buscar(@PathVariable Long id) {
        return converter(servico.buscarAtiva(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NoticiaResponse criar(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody NoticiaRequest dados) {
        return converter(servico.criar(dados, usuario.getId()));
    }

    @PutMapping("/{id}")
    public NoticiaResponse editar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody NoticiaRequest dados) {
        return converter(servico.editar(id, dados, usuario.getId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        servico.apagar(id, usuario.getId());
    }

    private NoticiaResponse converter(Noticia noticia) {
        return new NoticiaResponse(noticia.getId(), noticia.getTitulo(), noticia.getConteudo(),
                noticia.getIdAutor(), noticia.getCriadoEm(), noticia.getAtualizadoEm());
    }
}
