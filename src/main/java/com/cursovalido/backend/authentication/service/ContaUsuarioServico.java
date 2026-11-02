package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.dto.DadosPessoaisResposta;
import com.cursovalido.backend.authentication.dto.EnderecoRequisicao;
import com.cursovalido.backend.authentication.entity.AceiteTermoArquivado;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.AceiteTermoArquivadoRepositorio;
import com.cursovalido.backend.authentication.repository.LogAuditoriaRepositorio;
import com.cursovalido.backend.authentication.repository.SessaoRepositorio;
import com.cursovalido.backend.authentication.repository.TokenEmailRepositorio;
import com.cursovalido.backend.authentication.repository.UsuarioRepositorio;
import com.cursovalido.backend.forum.repository.ComentarioRepositorio;
import com.cursovalido.backend.forum.repository.TopicoRepositorio;
import com.cursovalido.backend.forum.repository.UsuarioForumRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContaUsuarioServico {
    private final UsuarioRepositorio repositorio;
    private final TokenEmailRepositorio repositorioTokens;
    private final LogAuditoriaRepositorio repositorioAuditoria;
    private final AceiteTermoArquivadoRepositorio repositorioAceitesArquivados;
    private final ComentarioRepositorio comentarios;
    private final TopicoRepositorio topicos;
    private final UsuarioForumRepositorio usuariosForumRepositorio;
    private final SessaoRepositorio sessoes;

    @Transactional(readOnly = true)
    public DadosPessoaisResposta consultarDados(Long idUsuario) {
        Usuario usuario = repositorio.findById(idUsuario)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado"));
        var endereco = usuario.getEndereco();
        var respostaEndereco = endereco == null ? null
                : new EnderecoRequisicao(endereco.getCep(),
                        endereco.getLogradouro(), endereco.getBairro(), endereco.getCidade(), endereco.getEstado());
        return new DadosPessoaisResposta(usuario.getId(),
                usuario.getNomeCompleto(), usuario.getEmail(), usuario.getCpf(), usuario.getDataNascimento(),
                usuario.getTelefone(), usuario.getMatricula(), respostaEndereco, usuario.getPerfil(),
                usuario.isAtivo(), usuario.isEmailConfirmado(), usuario.getStatusAprovacao(),
                usuario.getDeclarouMaiorIdade(), usuario.getAceitouTermosLgpd(), usuario.getVersaoTermos(),
                usuario.getDataHoraAceite(),
                repositorioAceitesArquivados.findByUsuarioIdOriginalOrderByDataHoraArquivamentoAsc(idUsuario).stream()
                        .map(aceite -> new DadosPessoaisResposta.Consentimento(
                                aceite.getVersao(), aceite.getDataHoraAceite(), aceite.getDataHoraArquivamento(), aceite.getMotivo()))
                        .toList(),
                repositorioAuditoria.findByUsuarioIdOrderByDataHoraAsc(idUsuario).stream()
                        .map(log -> new DadosPessoaisResposta.RegistroAuditoria(
                                log.getTipoAcao(), log.getDetalhes(), log.getEnderecoIp(), log.getDataHora()))
                        .toList());
    }

    @Transactional
    // Remove a conta e trata os registros ligados ao usuario.
    public void excluirConta(Long idUsuario) {
        Usuario usuario = repositorio.findById(idUsuario)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado"));
        var topicosDoUsuario = topicos.findByIdAutor(idUsuario);
        for (var topico : topicosDoUsuario)
            comentarios.deleteByTopicoId(topico.getId());
        comentarios.deleteByIdAutor(idUsuario);
        topicosDoUsuario.forEach(topico -> topicos.deleteById(topico.getId()));
        repositorioTokens.deleteByUsuarioId(idUsuario);
        sessoes.deleteByUsuarioId(idUsuario);
        if (usuario.getVersaoTermos() != null && usuario.getDataHoraAceite() != null)
            repositorioAceitesArquivados.save(new AceiteTermoArquivado(
                usuario.getId(), usuario.getVersaoTermos(), usuario.getDataHoraAceite(), "EXCLUSAO_CONTA"));
        repositorioAuditoria.anonimizarPorUsuarioId(idUsuario);
        usuariosForumRepositorio.deleteById(idUsuario);
        repositorio.deleteById(idUsuario);
    }
}
