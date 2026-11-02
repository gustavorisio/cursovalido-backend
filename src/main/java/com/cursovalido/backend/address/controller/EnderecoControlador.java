package com.cursovalido.backend.address.controller;

import com.cursovalido.backend.address.dto.EnderecoResposta;
import com.cursovalido.backend.address.service.EnderecoServico;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/enderecos")
public class EnderecoControlador {
    @Autowired
    private EnderecoServico servico;

    @GetMapping("/consultar/{cep}")
    public EnderecoResposta consultar(@PathVariable String cep, HttpServletRequest request) {
        return servico.consultar(cep, request.getRemoteAddr());
    }
}