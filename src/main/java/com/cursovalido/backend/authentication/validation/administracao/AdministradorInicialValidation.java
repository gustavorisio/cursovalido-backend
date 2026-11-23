package com.cursovalido.backend.authentication.validation.administracao;

import com.cursovalido.backend.authentication.dto.ConviteAcessoRequest;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.validation.senha.RegrasSenha;
import jakarta.validation.Validator;

public final class AdministradorInicialValidation {
    private AdministradorInicialValidation() {
    }

    public static void validarConfiguracao(String endereco, String senha, Validator validador,
            UsuarioRepository usuarios) {
        var configuracao = new ConviteAcessoRequest(endereco, Perfil.ADMINISTRADOR);
        if (!validador.validate(configuracao).isEmpty()) {
            throw new IllegalStateException("Defina ADMIN_EMAIL com um endereco de e-mail valido.");
        }
        RegrasSenha.primeiroProblema(senha).ifPresent(problema -> {
            throw new IllegalStateException("ADMIN_PASSWORD invalida: " + problema);
        });
        if (usuarios.existsByEmail(endereco)) {
            throw new IllegalStateException("ADMIN_EMAIL ja pertence a uma conta. Nenhuma conta sera promovida.");
        }
    }
}
