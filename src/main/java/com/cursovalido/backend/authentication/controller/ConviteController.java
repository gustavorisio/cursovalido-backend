package com.cursovalido.backend.authentication.controller;

import com.cursovalido.backend.authentication.dto.ConviteCadastroRequisicao;
import com.cursovalido.backend.authentication.dto.TokenEmailRequisicao;
import com.cursovalido.backend.authentication.service.ConviteAcessoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/convites")
@RequiredArgsConstructor
public class ConviteController {
    private final ConviteAcessoService convites;

    @PostMapping("/confirmar-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmarEmail(@Valid @RequestBody TokenEmailRequisicao requisicao) {
        convites.confirmarEmail(requisicao.token());
    }

    @PostMapping("/completar-cadastro")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void completar(@Valid @RequestBody ConviteCadastroRequisicao requisicao) {
        convites.completar(requisicao.token(), requisicao.cadastro());
    }
}
