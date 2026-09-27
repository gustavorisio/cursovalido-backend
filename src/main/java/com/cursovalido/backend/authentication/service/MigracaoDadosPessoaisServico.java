package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.UsuarioRepositorio;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MigracaoDadosPessoaisServico {
    private final UsuarioRepositorio usuarios;

    public MigracaoDadosPessoaisServico(UsuarioRepositorio usuarios) {
        this.usuarios = usuarios;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void criptografarDadosLegados() {
        for (Usuario usuario : usuarios.findAll()) {
            usuarios.save(usuario);
        }
    }
}