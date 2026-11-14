package com.cursovalido.backend.aulas.controller;

import com.cursovalido.backend.aulas.dto.*;
import com.cursovalido.backend.aulas.entity.*;
import com.cursovalido.backend.aulas.service.AdministradorAulasService;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/administracao")
@RequiredArgsConstructor
public class AdministradorAulasController {
    private final AdministradorAulasService servico;

    @PutMapping("/cursos/{id}")
    public CursoResponse editarCurso(@PathVariable Long id, @Valid @RequestBody CursoRequest dados,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return converter(servico.editarCurso(id, dados, usuario.getId()));
    }

    @DeleteMapping("/cursos/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void excluirCurso(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        servico.excluirCurso(id, usuario.getId());
    }

    @GetMapping("/cursos")
    public List<CursoResponse> listarCursos(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.listarCursos(usuario.getId()).stream().map(this::converter).toList();
    }

    @GetMapping("/cursos/{id}")
    public CursoResponse buscarCurso(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return converter(servico.buscarCurso(id, usuario.getId()));
    }

    @GetMapping("/cursos/{idCurso}/modulos")
    public List<ModuloResponse> listarModulos(@PathVariable Long idCurso,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.listarModulos(idCurso, usuario.getId()).stream().map(this::converter).toList();
    }

    @GetMapping("/modulos/{id}/aulas")
    public List<AulaAdministrativaResponse> listarAulas(@PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.listarAulas(id, usuario.getId()).stream().map(this::converter).toList();
    }

    @GetMapping("/aulas/{id}")
    public AulaAdministrativaResponse buscarAula(@PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return converter(servico.buscarAula(id, usuario.getId()));
    }

    private CursoResponse converter(Curso curso) {
        return CursoResponse.de(curso);
    }

    private ModuloResponse converter(Modulo modulo) {
        return new ModuloResponse(modulo.getId(), modulo.getTitulo(), modulo.getDescricao(), modulo.getIdCurso(),
                modulo.getOrdem());
    }

    public record AulaAdministrativaResponse(Long id, String titulo, String descricao, Long idModulo,
            int ordem, String youtubeVideoId, boolean ativo) {
    }

    @PutMapping("/aulas/{id}")
    public AulaAdministrativaResponse editarAula(@PathVariable Long id, @Valid @RequestBody AulaRequest dados,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return converter(servico.editarAula(id, dados, usuario.getId()));
    }

    @PutMapping("/aulas/{id}/arquivar")
    public AulaAdministrativaResponse arquivarAula(@PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return converter(servico.alterarDisponibilidade(id, false, usuario.getId()));
    }

    @PutMapping("/aulas/{id}/reabrir")
    public AulaAdministrativaResponse reabrirAula(@PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return converter(servico.alterarDisponibilidade(id, true, usuario.getId()));
    }

    private AulaAdministrativaResponse converter(Aula aula) {
        return new AulaAdministrativaResponse(aula.getId(), aula.getTitulo(), aula.getDescricao(), aula.getIdModulo(),
                aula.getOrdem(), aula.getYoutubeVideoId(), aula.isAtivo());
    }
}
