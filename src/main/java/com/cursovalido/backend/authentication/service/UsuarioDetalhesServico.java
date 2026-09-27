package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.UsuarioRepositorio;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class UsuarioDetalhesServico implements UserDetailsService {
        @Autowired
        private UsuarioRepositorio repositorio;

        public UsuarioDetalhesServico() {
        }

        @Override
        // Busca o usuario usado pelo Spring Security na autenticacao.
        public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
                Usuario usuario = repositorio.findByEmail(email)
                                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado"));
                return new UsuarioAutenticado(usuario.getId(), usuario.getEmail(), usuario.getSenha(),
                                usuario.getPerfil().name(), usuario.isEmailConfirmado(),
                                Boolean.TRUE.equals(usuario.getAceitouTermosLgpd()), usuario.isAtivo()
                                                && usuario
                                                                .getStatusAprovacao() == com.cursovalido.backend.authentication.entity.StatusAprovacao.APROVADO);
        }
}