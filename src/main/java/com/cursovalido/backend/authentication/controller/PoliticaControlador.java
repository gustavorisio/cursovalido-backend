package com.cursovalido.backend.authentication.controller;

import com.cursovalido.backend.authentication.dto.PoliticaResposta;
import com.cursovalido.backend.authentication.dto.PoliticaRequisicao;
import com.cursovalido.backend.authentication.service.PoliticaServico;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/politicas")
public class PoliticaControlador {
    @Autowired
    private PoliticaServico servico;

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