package com.cursovalido.backend.address.controller;

import com.cursovalido.backend.address.dto.EnderecoResponse;
import com.cursovalido.backend.address.service.EnderecoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/enderecos")
public class EnderecoController {
    @Autowired
    private EnderecoService servico;

    @GetMapping("/consultar/{cep}")
    public EnderecoResponse consultar(@PathVariable String cep, HttpServletRequest request) {
        return servico.consultar(cep, request.getRemoteAddr());
    }
}