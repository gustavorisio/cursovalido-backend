package com.cursovalido.backend.authentication.security;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UsuarioAutenticado implements UserDetails {
    private final Long id;
    private final String email;
    private final String senha;
    private final String perfil;
    private final boolean emailConfirmado;
    private final boolean termosAceitos;
    private final boolean ativo;

    public UsuarioAutenticado(Long id, String email, String senha, String perfil,
            boolean emailConfirmado, boolean termosAceitos, boolean ativo) {
        this.id = id;
        this.email = email;
        this.senha = senha;
        this.perfil = perfil;
        this.emailConfirmado = emailConfirmado;
        this.termosAceitos = termosAceitos;
        this.ativo = ativo;
    }

    public Long getId() {
        return id;
    }

    public String getPerfil() {
        return perfil;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (!termosAceitos)
            return java.util.List.of(() -> "TERMOS_PENDENTES");
        if (!ativo)
            return java.util.List.of();
        return java.util.List.of(() -> "ROLE_" + perfil);
    }

    @Override
    public String getPassword() {
        return senha;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return emailConfirmado;
    }
}