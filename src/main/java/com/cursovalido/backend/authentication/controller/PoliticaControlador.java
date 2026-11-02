package com.cursovalido.backend.authentication.controller;

import com.cursovalido.backend.authentication.dto.PoliticaRequisicao;
import com.cursovalido.backend.authentication.dto.PoliticaResposta;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import com.cursovalido.backend.authentication.service.PoliticaServico;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/politicas")
@RequiredArgsConstructor
public class PoliticaControlador {
    private final PoliticaServico servico;

    @GetMapping("/ativas")
    public List<PoliticaResposta> listarAtivas() {
        return servico.listarAtivas();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PoliticaResposta publicar(@AuthenticationPrincipal UsuarioAutenticado administrador,
            @Valid @RequestBody PoliticaRequisicao requisicao) {
        return servico.publicar(requisicao, administrador.getId());
    }
}
