package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.dto.DadosPessoaisResponse;
import com.cursovalido.backend.authentication.entity.AceiteTermoArquivado;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.AceiteTermoArquivadoRepository;
import com.cursovalido.backend.authentication.repository.LogAuditoriaRepository;
import com.cursovalido.backend.authentication.repository.SessaoRepository;
import com.cursovalido.backend.authentication.repository.TokenEmailRepository;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.repository.ConviteAcessoRepository;
import com.cursovalido.backend.forum.repository.ComentarioRepository;
import com.cursovalido.backend.forum.repository.TopicoRepository;
import com.cursovalido.backend.forum.repository.UsuarioForumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContaUsuarioService {
    private final UsuarioRepository repositorio;
    private final IdentidadeExcluidaService identidades;
    private final TokenEmailService tokensEmail;
    private final EmailService emails;
    private final TokenEmailRepository repositorioTokens;
    private final LogAuditoriaRepository repositorioAuditoria;
    private final AceiteTermoArquivadoRepository repositorioAceitesArquivados;
    private final ComentarioRepository comentarios;
    private final TopicoRepository topicos;
    private final UsuarioForumRepository usuariosForumRepositorio;
    private final SessaoRepository sessoes;
    private final ConviteAcessoRepository convites;

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
                usuario.getNomeCompleto(), usuario.getEmail(), usuario.getCpf(), usuario.getDataNascimento(),
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
        Usuario usuario = repositorio.findById(idUsuario).orElseThrow();
        emails.enviarExclusaoConta(usuario, tokensEmail.criarExclusao(usuario));
    }

    @Transactional
    public void excluirConta(Long idUsuario, String codigo) {
        tokensEmail.validarExclusao(idUsuario, codigo);
        Usuario usuario = repositorio.findById(idUsuario)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado"));
        identidades.registrar(usuario);
        var topicosDoUsuario = topicos.findByIdAutor(idUsuario);
        for (var topico : topicosDoUsuario)
            comentarios.deleteByTopicoId(topico.getId());
        comentarios.deleteByIdAutor(idUsuario);
        topicosDoUsuario.forEach(topico -> topicos.deleteById(topico.getId()));
        repositorioTokens.deleteByUsuarioId(idUsuario);
        sessoes.deleteByUsuarioId(idUsuario);
        convites.deleteByEmail(usuario.getEmail());
        arquivarAceite(usuario);
        repositorioAuditoria.anonimizarPorUsuarioId(idUsuario);
        usuariosForumRepositorio.deleteById(idUsuario);
        repositorio.deleteById(idUsuario);
    }
}
