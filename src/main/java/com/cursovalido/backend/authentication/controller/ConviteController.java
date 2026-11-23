package com.cursovalido.backend.authentication.controller;

import com.cursovalido.backend.authentication.dto.ConviteCadastroRequest;
import com.cursovalido.backend.authentication.dto.ConviteDadosResponse;
import com.cursovalido.backend.authentication.dto.TokenEmailRequest;
import com.cursovalido.backend.authentication.service.ConviteAcessoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/convites")
@RequiredArgsConstructor
public class ConviteController {
    private final ConviteAcessoService convites;

    @PostMapping("/consultar")
    public ConviteDadosResponse consultar(
            @Valid @RequestBody TokenEmailRequest requisicao) {
        return convites.consultar(requisicao.token());
    }

    @PostMapping("/completar-cadastro")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void completar(@Valid @RequestBody ConviteCadastroRequest requisicao,
            HttpServletRequest request) {
        convites.completar(requisicao.token(), requisicao.cadastro(), request.getRemoteAddr());
    }
}
