package com.cursovalido.backend.authentication.controller;

import com.cursovalido.backend.authentication.dto.TermoResponsabilidadeResponse;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.service.ResponsabilidadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/politicas/responsabilidade")
@RequiredArgsConstructor
public class ResponsabilidadeController {
    private final ResponsabilidadeService responsabilidade;

    @GetMapping("/{perfil}")
    public TermoResponsabilidadeResponse consultar(@PathVariable Perfil perfil) {
        return responsabilidade.documento(perfil);
    }
}
