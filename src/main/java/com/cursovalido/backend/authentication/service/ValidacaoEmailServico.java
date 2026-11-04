package com.cursovalido.backend.authentication.service;

import java.net.IDN;
import java.util.regex.Pattern;
import javax.naming.NamingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ValidacaoEmailServico {
    private static final Pattern LOCAL = Pattern.compile(
            "[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*");
    private final DominioEmailServico dominios;

    public void validar(String email) {
        if (email == null || email.length() > 254)
            throw invalido();
        int separador = email.indexOf('@');
        if (separador < 1 || separador > 64 || !LOCAL.matcher(email.substring(0, separador)).matches())
            throw invalido();
        String dominio;
        try {
            dominio = IDN.toASCII(email.substring(separador + 1), IDN.USE_STD3_ASCII_RULES);
        } catch (IllegalArgumentException excecao) {
            throw invalido();
        }
        if (!dominio.contains(".") || dominio.endsWith(".") || dominio.length() > 253)
            throw invalido();
        try {
            if (!dominios.aceitaEmail(dominio))
                throw invalido();
        } catch (NamingException excecao) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Nao foi possivel validar o dominio do e-mail. Tente novamente mais tarde.", excecao);
        }
    }

    private ResponseStatusException invalido() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "E-mail invalido. Verifique o endereco e o dominio informado.");
    }
}
