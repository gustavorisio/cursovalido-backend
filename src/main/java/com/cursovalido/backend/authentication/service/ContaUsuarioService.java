package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.dto.DadosPessoaisResponse;
import com.cursovalido.backend.authentication.entity.AceiteTermoArquivado;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.AceiteTermoArquivadoRepository;
import com.cursovalido.backend.authentication.repository.LogAuditoriaRepository;
import com.cursovalido.backend.authentication.repository.SessaoRepository;
import com.cursovalido.backend.authentication.repository.TokenEmailRepository;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.validation.conta.ContaValidation;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.validation.administracao.PrivilegiosAdministrativosService;
import com.cursovalido.backend.forum.repository.UsuarioForumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContaUsuarioService {
    private final UsuarioRepository repositorio;
    private final IdentidadeExcluidaService identidades;
    private final LogAuditoriaService auditoria;
    private final TokenEmailService tokensEmail;
    private final EmailService emails;
    private final TokenEmailRepository repositorioTokens;
    private final LogAuditoriaRepository repositorioAuditoria;
    private final AceiteTermoArquivadoRepository repositorioAceitesArquivados;
    private final UsuarioForumRepository usuariosForumRepositorio;
    private final SessaoRepository sessoes;
    private final PrivilegiosAdministrativosService privilegios;

    private void arquivarAceite(Usuario usuario) {
        if (usuario.getVersaoTermos() == null) {
            return;
        }
        if (usuario.getDataHoraAceite() == null) {
            return;
        }
        repositorioAceitesArquivados.save(new AceiteTermoArquivado(
                usuario.getId(), usuario.getVersaoTermos(), usuario.getDataHoraAceite(), "EXCLUSAO_CONTA"));
    }

    @Transactional(readOnly = true)
    public DadosPessoaisResponse consultarDados(Long idUsuario) {
        return consultarDados(idUsuario, true);
    }

    @Transactional(readOnly = true)
    public DadosPessoaisResponse consultarDadosParaExportacao(Long idUsuario) {
        return consultarDados(idUsuario, false);
    }

    private DadosPessoaisResponse consultarDados(Long idUsuario, boolean incluirAuditoria) {
        Usuario usuario = repositorio.findById(idUsuario)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado"));
        return new DadosPessoaisResponse(usuario.getId(),
                usuario.getNomeCompleto(), usuario.getEmail(), usuario.getDataNascimento(),
                usuario.getMatricula(), usuario.getPerfil(),
                usuario.isAtivo(), usuario.isEmailConfirmado(), usuario.getStatusAprovacao(),
                usuario.getDeclarouMaiorIdade(), usuario.getAceitouTermosLgpd(), usuario.getVersaoTermos(),
                usuario.getDataHoraAceite(),
                repositorioAceitesArquivados.findByUsuarioIdOriginalOrderByDataHoraArquivamentoAsc(idUsuario).stream()
                        .map(aceite -> new DadosPessoaisResponse.Consentimento(
                                aceite.getVersao(), aceite.getDataHoraAceite(), aceite.getDataHoraArquivamento(),
                                aceite.getMotivo()))
                        .toList(),
                incluirAuditoria ? repositorioAuditoria.findByUsuarioIdOrderByDataHoraAsc(idUsuario).stream()
                        .map(log -> new DadosPessoaisResponse.RegistroAuditoria(
                                log.getTipoAcao(), log.getDetalhes(), log.getEnderecoIp(), log.getDataHora()))
                        .toList() : null);
    }

    @Transactional
    // Remove a conta e trata os registros ligados ao usuario.
    public void solicitarExclusao(Long idUsuario) {
        Usuario usuario = repositorio.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado"));
        emails.enviarExclusaoConta(usuario, tokensEmail.criarExclusao(usuario));
    }

    @Transactional
    public void excluirConta(Long idUsuario, String codigo) {
        tokensEmail.validarExclusao(idUsuario, codigo);
        Usuario usuario = repositorio.findById(idUsuario)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado"));
        encerrarConta(usuario);
    }

    @Transactional
    public void excluirPelaAdministracao(Long idUsuario, Long idAdministrador) {
        privilegios.verificarPrincipal(idAdministrador);
        if (idUsuario.equals(idAdministrador)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nao e permitido excluir a propria conta pelo painel");
        }
        Usuario usuario = repositorio.buscarParaAtualizar(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado"));
        if (privilegios.ehPrincipal(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nao e permitido excluir o administrador principal");
        }
        if (usuario.getPerfil() != Perfil.PROFESSOR && usuario.getPerfil() != Perfil.ADMINISTRADOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A exclusao administrativa e restrita a equipe");
        }
        if (usuario.getExcluidoEm() != null) return;
        encerrarConta(usuario);
        auditoria.registrar(idAdministrador, "EXCLUSAO_ADMINISTRATIVA_CONTA",
                "Usuario " + idUsuario + "; perfil " + usuario.getPerfil(), null);
    }

    @Transactional
    public void excluirPendencia(Long idUsuario) {
        Usuario usuario = repositorio.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado"));
        ContaValidation.validarPendencia(usuario);
        usuario.setStatusAprovacao(com.cursovalido.backend.authentication.entity.StatusAprovacao.REJEITADO);
        encerrarConta(usuario);
    }

    private void encerrarConta(Usuario usuario) {
        Long idUsuario = usuario.getId();
        ContaValidation.validarExclusao(usuario);
        identidades.registrar(usuario);
        auditoria.registrar(idUsuario, "EXCLUSAO_CONTA", "Conta anonimizada e acessos revogados", null);
        repositorioTokens.deleteByUsuarioId(idUsuario);
        sessoes.deleteByUsuarioId(idUsuario);
        arquivarAceite(usuario);
        usuario.setAtivo(false);
        usuario.setEmailConfirmado(false);
        usuario.setExcluidoEm(java.time.LocalDateTime.now());
        usuario.setNomeCompleto("Usuario excluido");
        usuario.setEmail("excluido-" + idUsuario + "@conta.invalid");
        usuario.setSenha("");
        usuario.setDataNascimento(null);
        usuario.setMatricula("excluido-" + idUsuario);
        repositorio.saveAndFlush(usuario);
        usuariosForumRepositorio.findById(idUsuario).ifPresent(forum ->
                usuariosForumRepositorio.sincronizar(idUsuario, usuario.getNomeCompleto(), usuario.getEmail(),
                        usuario.getPerfil().name(), false));
    }
}
