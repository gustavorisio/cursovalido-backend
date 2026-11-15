package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.ContaExcluida;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.ContaExcluidaRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IdentidadeExcluidaService {
    private final ContaExcluidaRepository registros;

    public List<Long> anteriores(String email) {
        return localizar(email).stream().map(ContaExcluida::getId).toList();
    }

    public void registrar(Usuario usuario) {
        var registro = new ContaExcluida();
        registro.setId(usuario.getId());
        registro.setEmailHash(hashEmail(usuario.getEmail()));
        registros.saveAndFlush(registro);
    }

    private List<ContaExcluida> localizar(String email) {
        return registros.findByEmailHash(hashEmail(email));
    }

    private String hashEmail(String email) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(email.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException erro) {
            throw new IllegalStateException("Nao foi possivel proteger o e-mail", erro);
        }
    }
}
