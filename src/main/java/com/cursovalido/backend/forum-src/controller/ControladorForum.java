package com.cursovalido.backend.forum.controller;

import com.cursovalido.backend.forum.entity.Comentario;
import com.cursovalido.backend.forum.entity.Topico;
import com.cursovalido.backend.forum.entity.UsuarioForum;
import com.cursovalido.backend.forum.dto.ComentarioRequest;
import com.cursovalido.backend.forum.dto.ComentarioResponse;
import com.cursovalido.backend.forum.dto.TopicoResumoDTO;
import com.cursovalido.backend.forum.dto.TopicoRequest;
import com.cursovalido.backend.forum.dto.TopicoResponse;
import com.cursovalido.backend.forum.dto.RespostaUsuariosForum;
import com.cursovalido.backend.forum.dto.UsuarioForumResponse;
import com.cursovalido.backend.forum.service.ComentarioServico;
import com.cursovalido.backend.forum.service.TopicoServico;
import com.cursovalido.backend.forum.service.UsuarioForumServico;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/forum")
public class ControladorForum {
    @Autowired
    private TopicoServico servicoTopicos;
    @Autowired
    private ComentarioServico servicoComentarios;
    @Autowired
    private UsuarioForumServico servicoUsuarios;

    @GetMapping("/topicos")
    public List<TopicoResumoDTO> listarTopicosRecentes(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "") String busca) {
        return servicoTopicos.listarRecentes(page, size, busca, usuario.getId());
    }

    @PostMapping("/topicos")
    public TopicoResponse criarTopico(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody TopicoRequest requisicao) {
        Topico topico = new Topico();
        topico.setTitulo(requisicao.titulo());
        topico.setDescricao(requisicao.descricao());
        return converter(servicoTopicos.criar(topico, usuario.getId()));
    }

    @PutMapping("/topicos/{id}")
    public TopicoResponse editarTopico(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody TopicoRequest requisicao) {
        Topico dadosAtualizados = new Topico();
        dadosAtualizados.setTitulo(requisicao.titulo());
        dadosAtualizados.setDescricao(requisicao.descricao());
        return converter(servicoTopicos.editar(id, dadosAtualizados, usuario.getId()));
    }

    @PutMapping("/topicos/{id}/arquivar")
    public void arquivarTopico(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        servicoTopicos.arquivar(id, usuario.getId());
    }

    @PutMapping("/topicos/{id}/fechar")
    public void fecharTopico(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        servicoTopicos.fechar(id, usuario.getId());
    }

    @DeleteMapping("/topicos/{id}")
    public void apagarTopico(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        servicoTopicos.apagar(id, usuario.getId());
    }

    @GetMapping("/topicos/{idTopico}/comentarios")
    public List<ComentarioResponse> listarComentarios(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long idTopico) {
        return servicoComentarios.listarPorTopico(idTopico, usuario.getId()).stream()
                .map(this::converter)
                .toList();
    }

    @PostMapping("/topicos/{idTopico}/comentarios")
    public ComentarioResponse adicionarComentario(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long idTopico,
            @Valid @RequestBody ComentarioRequest requisicao) {
        Comentario comentario = new Comentario();
        comentario.setConteudo(requisicao.conteudo());
        return converter(servicoComentarios.criar(idTopico, comentario, usuario.getId()));
    }

    @PutMapping("/comentarios/{id}")
    public ComentarioResponse editarComentario(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody ComentarioRequest requisicao) {
        Comentario dadosAtualizados = new Comentario();
        dadosAtualizados.setConteudo(requisicao.conteudo());
        return converter(servicoComentarios.editar(id, dadosAtualizados, usuario.getId()));
    }

    @DeleteMapping("/comentarios/{id}")
    public void apagarComentario(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        servicoComentarios.apagar(id, usuario.getId());
    }

    @DeleteMapping("/topicos/{idTopico}/comentarios/{id}")
    public void apagarComentarioDoTopico(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long idTopico, @PathVariable Long id) {
        servicoComentarios.apagarDoTopico(idTopico, id, usuario.getId());
    }

    @GetMapping("/users")
    public RespostaUsuariosForum listarUsuarios() {
        List<UsuarioForumResponse> usuarios = servicoUsuarios.listarAtivos().stream()
                .map(this::converter)
                .toList();
        return new RespostaUsuariosForum(usuarios);
    }

    private TopicoResponse converter(Topico topico) {
        return new TopicoResponse(topico.getId(), topico.getTitulo(), topico.getDescricao(), topico.getNomeAutor(),
                topico.getPapelAutor(), topico.getIdAutor(), topico.getCriadoEm(), topico.getQuantidadeRespostas(),
                topico.isAtivo(), topico.isFechado());
    }

    private ComentarioResponse converter(Comentario comentario) {
        return new ComentarioResponse(comentario.getId(), comentario.getConteudo(), comentario.getNomeAutor(),
                comentario.getPapelAutor(), comentario.getIdAutor(), comentario.getCriadoEm(), comentario.isAtivo());
    }

    private UsuarioForumResponse converter(UsuarioForum usuario) {
        return new UsuarioForumResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPerfil());
    }

}