package com.cursovalido.backend.notificacoes.service;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import com.cursovalido.backend.notificacoes.dto.*;
import com.cursovalido.backend.notificacoes.repository.ComunicadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ComunicadoService {
    private final ComunicadoRepository repositorio;
    private final UsuarioRepository usuarios;
    private final LogAuditoriaService auditoria;

    @Transactional
    public ComunicadoResponse publicar(Long administrador, ComunicadoRequest dados) {
        verificarPerfil(administrador, Perfil.ADMINISTRADOR);
        repositorio.bloquearPublicacoes();
        if (repositorio.contar() >= 30) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O limite de 30 comunicados foi atingido. Exclua um comunicado antes de publicar outro.");
        }
        if (repositorio.publicouRecentemente(administrador, java.time.LocalDateTime.now().minusSeconds(30))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Aguarde 30 segundos entre a criação de comunicados.");
        }
        var comunicado = repositorio.criar(dados.titulo().trim(), dados.conteudo().trim(), administrador);
        repositorio.registrarPublicacao(administrador, comunicado.criadoEm());
        auditoria.registrar(administrador, "PUBLICACAO_COMUNICADO", "Comunicado " + comunicado.id(), null);
        return comunicado;
    }

    @Transactional
    public ComunicadoResponse editar(Long administrador, Long id, ComunicadoRequest dados) {
        verificarPerfil(administrador, Perfil.ADMINISTRADOR);
        var comunicado = repositorio.editar(id, dados.titulo().trim(), dados.conteudo().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comunicado não encontrado."));
        auditoria.registrar(administrador, "EDICAO_COMUNICADO", "Comunicado " + id, null);
        return comunicado;
    }

    @Transactional
    public void excluir(Long administrador, Long id) {
        verificarPerfil(administrador, Perfil.ADMINISTRADOR);
        if (!repositorio.excluir(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comunicado não encontrado.");
        }
        auditoria.registrar(administrador, "EXCLUSAO_COMUNICADO", "Comunicado " + id, null);
    }

    @Transactional(readOnly = true)
    public PaginaComunicadosResponse listarAdministracao(Long usuario, int pagina, int tamanho) {
        verificarPerfil(usuario, Perfil.ADMINISTRADOR);
        return listar(usuario, pagina, tamanho, false);
    }

    @Transactional(readOnly = true)
    public PaginaComunicadosResponse listarNotificacoes(Long usuario, int pagina, int tamanho) {
        verificarContaAtiva(usuario);
        return listar(usuario, pagina, tamanho, true);
    }

    private PaginaComunicadosResponse listar(Long usuario, int pagina, int tamanho, boolean incluirNaoLidas) {
        if (pagina < 0 || tamanho < 1 || tamanho > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Página deve ser maior ou igual a zero e tamanho deve estar entre 1 e 50.");
        }
        long total = repositorio.contar();
        return new PaginaComunicadosResponse(repositorio.listar(usuario, pagina, tamanho), pagina,
                (int) Math.ceil((double) total / tamanho), total,
                incluirNaoLidas ? repositorio.contarNaoLidas(usuario) : 0);
    }

    @Transactional(readOnly = true)
    public long contarNaoLidas(Long usuario) {
        verificarContaAtiva(usuario);
        return repositorio.contarNaoLidas(usuario);
    }

    @Transactional
    public void marcarLida(Long usuario, Long id) {
        verificarContaAtiva(usuario);
        if (!repositorio.existe(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comunicado não encontrado.");
        }
        repositorio.marcarLida(id, usuario);
    }

    private void verificarPerfil(Long id, Perfil perfil) {
        if (verificarContaAtiva(id).getPerfil() != perfil) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso não permitido para esta conta.");
        }
    }

    private Usuario verificarContaAtiva(Long id) {
        var usuario = id == null ? null : usuarios.findById(id).orElse(null);
        if (usuario == null || !usuario.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso não permitido para esta conta.");
        }
        return usuario;
    }
}
