package com.cursovalido.backend.authentication.security;

import com.cursovalido.backend.authentication.entity.Perfil;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@RequiredArgsConstructor
public class UsuarioAutenticado implements UserDetails {
    private final Long id;
    private final String email;
    private final String senha;
    private final Perfil perfil;
    private final boolean emailConfirmado;
    private final boolean termosAceitos;
    private final boolean ativo;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (!termosAceitos)
            return List.of(() -> "TERMOS_PENDENTES");
        if (!ativo)
            return List.of();
        return List.of(() -> "ROLE_" + perfil.name());
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
        if (!emailConfirmado) {
            return false;
        }
        return ativo;
    }
}
