package com.cursovalido.backend.notificacoes.service;

import com.cursovalido.backend.authentication.entity.Perfil;
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
        var comunicado = repositorio.criar(dados.titulo().trim(), dados.conteudo().trim(), administrador);
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
    public PaginaComunicadosResponse listar(Long usuario, Perfil perfil, int pagina, int tamanho) {
        verificarPerfil(usuario, perfil);
        if (pagina < 0 || tamanho < 1 || tamanho > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Página deve ser maior ou igual a zero e tamanho deve estar entre 1 e 50.");
        }
        long total = repositorio.contar();
        return new PaginaComunicadosResponse(repositorio.listar(usuario, pagina, tamanho), pagina,
                (int) Math.ceil((double) total / tamanho), total,
                perfil == Perfil.ALUNO ? repositorio.contarNaoLidas(usuario) : 0);
    }

    @Transactional(readOnly = true)
    public long contarNaoLidas(Long aluno) {
        verificarPerfil(aluno, Perfil.ALUNO);
        return repositorio.contarNaoLidas(aluno);
    }

    @Transactional
    public void marcarLida(Long aluno, Long id) {
        verificarPerfil(aluno, Perfil.ALUNO);
        if (!repositorio.existe(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comunicado não encontrado.");
        }
        repositorio.marcarLida(id, aluno);
    }

    private void verificarPerfil(Long id, Perfil perfil) {
        var usuario = id == null ? null : usuarios.findById(id).orElse(null);
        if (usuario == null || !usuario.isAtivo() || usuario.getPerfil() != perfil) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso não permitido para esta conta.");
        }
    }
}
