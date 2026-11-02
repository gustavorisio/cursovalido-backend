package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.SessaoRepositorio;
import com.cursovalido.backend.authentication.repository.UsuarioRepositorio;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdministradorInicialServico {
    public static final String EMAIL = "cursovalido@gmail.com";
    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder senhas;
    @Value("${aplicacao.admin.senha:}")
    private String senha;
    private final SessaoRepositorio sessoes;
    private final TokenEmailServico tokensEmail;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void criar() {
        if (senha.isEmpty())
            return;
        if (senha.isBlank() || senha.length() < 8 || senha.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalStateException("ADMIN_PASSWORD deve ter pelo menos 8 caracteres e no maximo 72 bytes.");
        var existente = usuarios.findByEmail(EMAIL);
        if (existente.isPresent()) {
            if (existente.get().getPerfil() != Perfil.ADMINISTRADOR)
                throw new IllegalStateException("O e-mail do administrador inicial ja pertence a outro perfil.");
            Usuario administrador = existente.get();
            if (!senhas.matches(senha, administrador.getSenha())) {
                administrador.setSenha(senhas.encode(senha));
                sessoes.revogarPorUsuario(administrador.getId());
                tokensEmail.invalidarAcessos(administrador);
                usuarios.save(administrador);
            }
            return;
        }

        Usuario administrador = new Usuario();
        administrador.setNomeCompleto("Administrador Curso Valido");
        administrador.setEmail(EMAIL);
        administrador.setSenha(senhas.encode(senha));
        administrador.setMatricula("ADMIN-" + UUID.randomUUID());
        administrador.setPerfil(Perfil.ADMINISTRADOR);
        administrador.setEmailConfirmado(true);
        administrador.setDeclarouMaiorIdade(false);
        administrador.setAceitouTermosLgpd(false);
        usuarios.save(administrador);
    }
}
