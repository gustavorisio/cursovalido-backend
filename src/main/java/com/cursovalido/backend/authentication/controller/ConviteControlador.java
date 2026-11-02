package com.cursovalido.backend.authentication.controller;

import com.cursovalido.backend.authentication.dto.ConviteCadastroRequisicao;
import com.cursovalido.backend.authentication.dto.TokenEmailRequisicao;
import com.cursovalido.backend.authentication.service.ConviteAcessoServico;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/convites")
public class ConviteControlador {
    @Autowired
    private ConviteAcessoServico convites;

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
