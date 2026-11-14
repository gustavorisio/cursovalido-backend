package com.cursovalido.backend.aulas.controller;

import com.cursovalido.backend.aulas.dto.*;
import com.cursovalido.backend.aulas.entity.SecaoCaminhos;
import com.cursovalido.backend.aulas.service.RoadmapService;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RoadmapController {
    private final RoadmapService servico;

    @GetMapping("/catalogo/roadmap")
    public PaginaCaminhosResponse consultar() { return servico.consultar(); }

    @GetMapping("/catalogo/roadmap/questionarios/{id}")
    public CaminhosResponse consultarQuestionario(@PathVariable Long id) { return servico.consultarQuestionario(id, null); }

    @PostMapping("/catalogo/roadmap/questionarios/{id}/resultado")
    public ResultadoCaminhosResponse calcular(@PathVariable Long id, @Valid @RequestBody RespostasCaminhosRequest dados) {
        return servico.calcular(id, dados, null);
    }

    @GetMapping("/aluno/roadmap/questionarios/{id}/resultado")
    public ResultadoCaminhosResponse consultarResultado(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.consultarResultado(id, usuario.getId());
    }

    @PostMapping("/aluno/roadmap/questionarios/{id}/resultado")
    public ResultadoCaminhosResponse responder(@PathVariable Long id, @Valid @RequestBody RespostasCaminhosRequest dados, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.calcular(id, dados, usuario.getId());
    }

    @GetMapping("/administracao/caminhos")
    public PaginaCaminhosResponse administracao(@AuthenticationPrincipal UsuarioAutenticado usuario) { return servico.consultarAdministracao(usuario.getId()); }

    @GetMapping("/administracao/caminhos/questionarios/{id}")
    public CaminhosResponse consultarGestao(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) { return servico.consultarQuestionario(id, usuario.getId()); }

    @PostMapping("/administracao/caminhos/questionarios")
    @ResponseStatus(HttpStatus.CREATED)
    public CaminhosResponse criar(@Valid @RequestBody CaminhosRequest dados, @AuthenticationPrincipal UsuarioAutenticado usuario) { return servico.salvarQuestionario(null, dados, usuario.getId()); }

    @PutMapping("/administracao/caminhos/questionarios/{id}")
    public CaminhosResponse editar(@PathVariable Long id, @Valid @RequestBody CaminhosRequest dados, @AuthenticationPrincipal UsuarioAutenticado usuario) { return servico.salvarQuestionario(id, dados, usuario.getId()); }

    @DeleteMapping("/administracao/caminhos/questionarios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) { servico.excluirQuestionario(id, usuario.getId()); }

    @PostMapping("/administracao/caminhos/secoes")
    @ResponseStatus(HttpStatus.CREATED)
    public SecaoCaminhos criarSecao(@Valid @RequestBody SecaoCaminhosRequest dados, @AuthenticationPrincipal UsuarioAutenticado usuario) { return servico.salvarSecao(null, dados, usuario.getId()); }

    @PutMapping("/administracao/caminhos/secoes/{id}")
    public SecaoCaminhos editarSecao(@PathVariable Long id, @Valid @RequestBody SecaoCaminhosRequest dados, @AuthenticationPrincipal UsuarioAutenticado usuario) { return servico.salvarSecao(id, dados, usuario.getId()); }

    @DeleteMapping("/administracao/caminhos/secoes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirSecao(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) { servico.excluirSecao(id, usuario.getId()); }
}
