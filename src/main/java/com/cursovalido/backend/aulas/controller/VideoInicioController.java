package com.cursovalido.backend.aulas.controller;

import com.cursovalido.backend.aulas.dto.VideoInicioRequest;
import com.cursovalido.backend.aulas.dto.VideoInicioResponse;
import com.cursovalido.backend.aulas.service.VideoInicioService;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class VideoInicioController {
    private final VideoInicioService videos;

    @GetMapping("/api/inicio/video")
    public VideoInicioResponse consultar() {
        return videos.consultar();
    }

    @PutMapping("/api/administracao/inicio/video")
    public VideoInicioResponse atualizar(@AuthenticationPrincipal UsuarioAutenticado administrador,
            @Valid @RequestBody VideoInicioRequest dados) {
        return videos.atualizar(administrador.getId(), dados);
    }
}
