package com.cursovalido.backend.aulas.controller;

import com.cursovalido.backend.aulas.dto.ProgressoAulaRequest;
import com.cursovalido.backend.aulas.dto.ProgressoAulaResponse;
import com.cursovalido.backend.aulas.entity.ProgressoAula;
import com.cursovalido.backend.aulas.service.ProgressoAulaService;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/aluno/aulas/{idAula}")
@RequiredArgsConstructor
public class ProgressoAulaController {
    private final ProgressoAulaService servico;

    @GetMapping("/progresso")
    public ProgressoAulaResponse buscar(@PathVariable Long idAula,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return converter(servico.buscar(idAula, usuario.getId()));
    }

    @PutMapping("/progresso")
    public ProgressoAulaResponse salvar(@PathVariable Long idAula,
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ProgressoAulaRequest dados) {
        return converter(servico.salvarPosicao(idAula, dados, usuario.getId()));
    }

    @PostMapping("/conclusao")
    public ProgressoAulaResponse concluir(@PathVariable Long idAula,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return converter(servico.concluir(idAula, usuario.getId()));
    }

    private ProgressoAulaResponse converter(ProgressoAula progresso) {
        return new ProgressoAulaResponse(progresso.getIdAula(), progresso.getPosicaoSegundos(),
                progresso.isConcluida(), progresso.getConcluidaEm(), progresso.getAtualizadoEm());
    }
}
