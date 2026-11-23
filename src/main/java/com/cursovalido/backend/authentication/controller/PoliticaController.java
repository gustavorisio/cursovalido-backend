package com.cursovalido.backend.authentication.controller;

import com.cursovalido.backend.authentication.dto.PoliticaRequest;
import com.cursovalido.backend.authentication.dto.PoliticaResponse;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import com.cursovalido.backend.authentication.service.PoliticaService;
import com.cursovalido.backend.authentication.service.PrivacidadeService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/politicas")
@RequiredArgsConstructor
public class PoliticaController {
    private final PoliticaService servico;
    private final PrivacidadeService privacidade;

    @GetMapping("/privacidade/ativa")
    public ResponseEntity<PoliticaResponse> privacidadeAtiva() {
        PoliticaResponse atual = privacidade.buscarAtiva();
        return atual == null ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(atual);
    }

    @GetMapping("/historico")
    public List<PoliticaResponse> historicoTermos() {
        return servico.listarHistorico();
    }

    @GetMapping("/privacidade/historico")
    public List<PoliticaResponse> historicoPrivacidade() {
        return privacidade.listarHistorico();
    }

    @PostMapping("/privacidade")
    @ResponseStatus(HttpStatus.CREATED)
    public PoliticaResponse publicarPrivacidade(@AuthenticationPrincipal UsuarioAutenticado administrador,
            @Valid @RequestBody PoliticaRequest requisicao) {
        return privacidade.publicar(requisicao, administrador.getId());
    }

    @GetMapping("/ativas")
    public List<PoliticaResponse> listarAtivas() {
        return servico.listarAtivas();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PoliticaResponse publicar(@AuthenticationPrincipal UsuarioAutenticado administrador,
            @Valid @RequestBody PoliticaRequest requisicao) {
        return servico.publicar(requisicao, administrador.getId());
    }
}
