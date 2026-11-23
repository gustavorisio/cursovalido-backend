package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.StatusAprovacao;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UsuarioDetalhesService implements UserDetailsService {
        private final UsuarioRepository repositorio;
        private final ResponsabilidadeService responsabilidade;

        @Override
        // Busca o usuário usado pelo Spring Security na autenticação.
        public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
                Usuario usuario = repositorio.findByEmail(email)
                                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado"));
                boolean acessoPermitido = usuario.isAtivo();
                if (usuario.getStatusAprovacao() != StatusAprovacao.APROVADO) {
                        acessoPermitido = false;
                }
                return new UsuarioAutenticado(usuario.getId(), usuario.getEmail(), usuario.getSenha(),
                                usuario.getPerfil(), usuario.isEmailConfirmado(),
                                Boolean.TRUE.equals(usuario.getAceitouTermosLgpd())
                                                && responsabilidade.atualizado(usuario),
                                acessoPermitido, usuario.getVersaoTermos());
        }

        public UsuarioAutenticado loadUserForRequest(String email, Long idUsuario, String idSessao) {
                var dados = repositorio.carregarAutenticacao(idUsuario, email, idSessao, LocalDateTime.now(),
                                responsabilidade.documento(Perfil.PROFESSOR).versao())
                                .orElseThrow(() -> new UsernameNotFoundException("Sessao ou usuario invalido"));
                return new UsuarioAutenticado(dados.getId(), dados.getEmail(), dados.getSenha(),
                                Perfil.valueOf(dados.getPerfil()), Boolean.TRUE.equals(dados.getEmailConfirmado()),
                                Boolean.TRUE.equals(dados.getTermosAceitos()), Boolean.TRUE.equals(dados.getAtivo()),
                                dados.getVersaoTermos(), Boolean.TRUE.equals(dados.getVersaoTermosAtiva()),
                                Boolean.TRUE.equals(dados.getAdministradorPrincipal()));
        }
}
