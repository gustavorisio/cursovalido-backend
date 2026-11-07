package com.cursovalido.backend.noticias.service;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import com.cursovalido.backend.noticias.dto.NoticiaRequest;
import com.cursovalido.backend.noticias.entity.Noticia;
import com.cursovalido.backend.noticias.repository.NoticiaRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class NoticiaService {
    private final NoticiaRepository repositorio;
    private final UsuarioRepository usuarios;
    private final LogAuditoriaService auditoria;

    // Lista somente as noticias ativas, da mais recente para a mais antiga.
    public List<Noticia> listarRecentes(int pagina, int tamanho, String busca) {
        if (pagina < 0 || tamanho < 1 || tamanho > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Pagina deve ser maior ou igual a zero e tamanho deve estar entre 1 e 100");
        }
        String termo = busca == null ? "" : busca.trim();
        return repositorio.listarRecentes(PageRequest.of(pagina, tamanho), termo);
    }

    public Noticia buscarAtiva(Long id) {
        return repositorio.findById(id).filter(Noticia::isAtivo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Noticia nao encontrada"));
    }

    @Transactional
    public Noticia criar(NoticiaRequest dados, Long idAdministrador) {
        verificarAdministrador(idAdministrador);
        Noticia noticia = new Noticia();
        noticia.setTitulo(dados.titulo().trim());
        noticia.setConteudo(dados.conteudo().trim());
        noticia.setIdAutor(idAdministrador);
        noticia.setCriadoEm(LocalDateTime.now());
        repositorio.save(noticia);
        auditoria.registrar(idAdministrador, "CADASTRO_NOTICIA", "Noticia " + noticia.getId(), null);
        return noticia;
    }

    @Transactional
    public Noticia editar(Long id, NoticiaRequest dados, Long idAdministrador) {
        verificarAdministrador(idAdministrador);
        Noticia noticia = buscarAtiva(id);
        verificarAutoria(noticia, idAdministrador);
        noticia.setTitulo(dados.titulo().trim());
        noticia.setConteudo(dados.conteudo().trim());
        noticia.setAtualizadoEm(LocalDateTime.now());
        repositorio.save(noticia);
        auditoria.registrar(idAdministrador, "EDICAO_NOTICIA", "Noticia " + id, null);
        return noticia;
    }

    // Retira a noticia da listagem sem apagar o registro do banco.
    @Transactional
    public void apagar(Long id, Long idAdministrador) {
        verificarAdministrador(idAdministrador);
        Noticia noticia = buscarAtiva(id);
        verificarAutoria(noticia, idAdministrador);
        noticia.setAtivo(false);
        repositorio.save(noticia);
        auditoria.registrar(idAdministrador, "EXCLUSAO_NOTICIA", "Noticia " + id, null);
    }

    private void verificarAdministrador(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrador nao informado");
        }
        var usuario = usuarios.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrador nao encontrado"));
        if (!usuario.isAtivo() || usuario.getPerfil() != Perfil.ADMINISTRADOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso exclusivo de administrador ativo");
        }
    }

    private void verificarAutoria(Noticia noticia, Long idAdministrador) {
        if (!idAdministrador.equals(noticia.getIdAutor())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente o autor pode alterar a noticia");
        }
    }
}
