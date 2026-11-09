package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.repository.InicializacaoAdministradorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PrivilegiosAdministrativosService {
    private final UsuarioRepository usuarios;
    private final InicializacaoAdministradorRepository inicializacao;

    public Usuario verificarAdministrador(Long id) {
        Usuario usuario = usuarios.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrador nao encontrado"));
        if (!usuario.isAtivo())
            throw negado();
        if (usuario.getPerfil() != Perfil.ADMINISTRADOR)
            throw negado();
        return usuario;
    }

    public boolean ehPrincipal(Usuario usuario) {
        return inicializacao.findById(1L)
                .map(registro -> usuario.getId().equals(registro.getIdUsuario()))
                .orElse(false);
    }

    public void verificarPrincipal(Long id) {
        if (!ehPrincipal(verificarAdministrador(id)))
            throw negado();
    }

    private ResponseStatusException negado() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Esta operacao e exclusiva do administrador principal");
    }
}
