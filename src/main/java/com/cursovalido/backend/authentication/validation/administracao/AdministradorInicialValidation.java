package com.cursovalido.backend.authentication.validation.administracao;

import com.cursovalido.backend.authentication.dto.ConviteAcessoRequest;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.validation.senha.SenhaValida;
import com.cursovalido.backend.authentication.validation.senha.SenhaValidaValidator;

public final class AdministradorInicialValidation {
    private AdministradorInicialValidation() {
    }

    public static void validarConfiguracao(String endereco, String senha, jakarta.validation.Validator validador,
            UsuarioRepository usuarios) {
        var configuracao = new ConviteAcessoRequest(endereco, Perfil.ADMINISTRADOR);
        if (!validador.validate(configuracao).isEmpty()) {
            throw new IllegalStateException("Defina ADMIN_EMAIL com um endereco de e-mail valido.");
        }
        if (!new SenhaValidaValidator().isValid(senha, null))
            throw senhaInvalida();
        if (usuarios.existsByEmail(endereco)) {
            throw new IllegalStateException("ADMIN_EMAIL ja pertence a uma conta. Nenhuma conta sera promovida.");
        }
    }

    private static IllegalStateException senhaInvalida() {
        return new IllegalStateException(
                SenhaValida.MENSAGEM);
    }
}
