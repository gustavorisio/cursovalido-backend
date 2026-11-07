package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.StatusAprovacao;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioDetalhesService implements UserDetailsService {
        private final UsuarioRepository repositorio;

        @Override
        // Busca o usuario usado pelo Spring Security na autenticacao.
        public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
                Usuario usuario = repositorio.findByEmail(email)
                                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado"));
                boolean acessoPermitido = usuario.isAtivo();
                if (usuario.getStatusAprovacao() != StatusAprovacao.APROVADO) {
                        acessoPermitido = false;
                }
                return new UsuarioAutenticado(usuario.getId(), usuario.getEmail(), usuario.getSenha(),
                                usuario.getPerfil(), usuario.isEmailConfirmado(),
                                Boolean.TRUE.equals(usuario.getAceitouTermosLgpd()),
                                acessoPermitido);
        }
}
