package com.cursovalido.backend.aulas.controller;

import com.cursovalido.backend.aulas.dto.GamificacaoResponse;
import com.cursovalido.backend.aulas.service.GamificacaoService;
import com.cursovalido.backend.aulas.dto.RankingAlunoResponse;
import com.cursovalido.backend.aulas.service.RankingAlunoService;
import java.util.List;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GamificacaoController {
    private final GamificacaoService gamificacao;
    private final RankingAlunoService ranking;

    @GetMapping("/api/aluno/gamificacao")
    public GamificacaoResponse consultar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return gamificacao.consultar(usuario.getId());
    }

    @GetMapping("/api/ranking/alunos")
    public List<RankingAlunoResponse> ranking() {
        return ranking.listar();
    }
}
