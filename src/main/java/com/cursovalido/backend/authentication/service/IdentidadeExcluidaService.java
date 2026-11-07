package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.ContaExcluida;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.ContaExcluidaRepository;
import com.cursovalido.backend.authentication.security.ProtecaoDadosPessoais;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class IdentidadeExcluidaService {
    private final ContaExcluidaRepository registros;

    public List<Long> anteriores(String email, String cpf) {
        return localizar(email, cpf).stream().map(ContaExcluida::getId).toList();
    }

    public void verificarCadastro(String email, String cpf) {
        if (localizar(email, cpf).stream().anyMatch(r -> r.getBloqueadaAte().isAfter(Instant.now()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "E-mail ou CPF bloqueado por 15 dias apos a exclusao da conta.");
        }
    }

    public void registrar(Usuario usuario) {
        var registro = new ContaExcluida();
        registro.setId(usuario.getId());
        registro.setEmailHash(hashEmail(usuario.getEmail()));
        registro.setCpfHash(usuario.getCpf() == null ? null : ProtecaoDadosPessoais.hashCpf(usuario.getCpf()));
        registro.setBloqueadaAte(Instant.now().plus(15, ChronoUnit.DAYS));
        registros.saveAndFlush(registro);
    }

    private List<ContaExcluida> localizar(String email, String cpf) {
        var emails = new HashSet<String>();
        var cpfs = new HashSet<String>();
        var encontrados = new HashMap<Long, ContaExcluida>();
        emails.add(hashEmail(email));
        cpfs.add(cpf == null ? "" : ProtecaoDadosPessoais.hashCpf(cpf));
        boolean mudou;
        // Percorre tambem os vinculos de exclusoes sucessivas com outros identificadores.
        do {
            mudou = false;
            for (var item : registros.findByEmailHashInOrCpfHashIn(emails, cpfs)) {
                if (encontrados.putIfAbsent(item.getId(), item) == null) {
                    emails.add(item.getEmailHash());
                    if (item.getCpfHash() != null) cpfs.add(item.getCpfHash());
                    mudou = true;
                }
            }
        } while (mudou);
        return new ArrayList<>(encontrados.values());
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
