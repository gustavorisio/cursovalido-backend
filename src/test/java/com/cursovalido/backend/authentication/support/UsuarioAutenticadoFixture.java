package com.cursovalido.backend.authentication.support;

import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import static org.mockito.Mockito.*;

public final class UsuarioAutenticadoFixture {
    private UsuarioAutenticadoFixture() {}

    public static UsuarioAutenticado usuario() {
        var usuario = mock(UsuarioAutenticado.class);
        when(usuario.getId()).thenReturn(7L);
        return usuario;
    }
}
