package com.cursovalido.backend.forum.controller;

import lombok.RequiredArgsConstructor;

import com.cursovalido.backend.forum.entity.Comentario;
import com.cursovalido.backend.forum.entity.Topico;
import com.cursovalido.backend.forum.entity.UsuarioForum;
import com.cursovalido.backend.forum.dto.ComentarioRequest;
import com.cursovalido.backend.forum.dto.ComentarioResponse;
import com.cursovalido.backend.forum.dto.TopicoResumoDTO;
import com.cursovalido.backend.forum.dto.TopicoRequest;
import com.cursovalido.backend.forum.dto.TopicoResponse;
import com.cursovalido.backend.forum.dto.UsuariosForumResponse;
import com.cursovalido.backend.forum.dto.UsuarioForumResponse;
import com.cursovalido.backend.forum.service.ComentarioService;
import com.cursovalido.backend.forum.service.TopicoService;
import com.cursovalido.backend.forum.service.UsuarioForumService;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import com.cursovalido.backend.authentication.security.VisibilidadeEmail;
import com.cursovalido.backend.authentication.entity.Perfil;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
public class ForumController {
    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");

    private final TopicoService servicoTopicos;
    private final ComentarioService servicoComentarios;
    private final UsuarioForumService servicoUsuarios;

    @GetMapping("/topicos")
    public List<TopicoResumoDTO> listarTopicosRecentes(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String busca) {
        return servicoTopicos.listarRecentes(page, size, busca, usuario == null ? null : usuario.getId()).stream()
                .map(this::converter)
                .toList();
    }

    @GetMapping("/topicos/meus")
    public org.springframework.data.domain.Page<TopicoResumoDTO> listarMeusTopicos(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String busca) {
        return servicoTopicos.listarMeus(page, size, busca, usuario.getId()).map(this::converter);
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

    @GetMapping("/topicos/{idTopico}/comentarios")
    public List<ComentarioResponse> listarComentarios(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long idTopico) {
        return servicoComentarios.listarPorTopico(idTopico, usuario == null ? null : usuario.getId()).stream()
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
    public UsuariosForumResponse listarUsuarios(@AuthenticationPrincipal UsuarioAutenticado principal) {
        boolean administrador = principal != null && principal.getPerfil() == Perfil.ADMINISTRADOR;
        List<UsuarioForumResponse> usuarios = servicoUsuarios.listarAtivos().stream()
                .map(usuario -> converter(usuario, administrador))
                .toList();
        return new UsuariosForumResponse(usuarios);
    }

    private TopicoResumoDTO converter(TopicoResumoDTO topico) {
        return new TopicoResumoDTO(topico.id(), topico.titulo(), topico.descricao(), topico.nomeAutor(),
                topico.papelAutor(), topico.idAutor(), horarioDeBrasilia(topico.criadoEm()),
                topico.quantidadeRespostas(),
                topico.ativo(), topico.fechado());
    }

    private TopicoResponse converter(Topico topico) {
        return new TopicoResponse(topico.getId(), topico.getTitulo(), topico.getDescricao(), topico.getNomeAutor(),
                topico.getPapelAutor(), topico.getIdAutor(), horarioDeBrasilia(topico.getCriadoEm()),
                topico.getQuantidadeRespostas(), topico.isAtivo(), topico.isFechado());
    }

    private ComentarioResponse converter(Comentario comentario) {
        return new ComentarioResponse(comentario.getId(),
                comentario.isAtivo() ? comentario.getConteudo() : "Esta mensagem foi excluída.",
                comentario.getNomeAutor(),
                comentario.getPapelAutor(), comentario.getIdAutor(), horarioDeBrasilia(comentario.getCriadoEm()),
                comentario.isAtivo());
    }

    // As colunas created_at do fórum armazenam UTC sem informação de fuso.
    private static LocalDateTime horarioDeBrasilia(LocalDateTime dataUtc) {
        return dataUtc.atOffset(ZoneOffset.UTC).atZoneSameInstant(BRASILIA).toLocalDateTime();
    }

    private UsuarioForumResponse converter(UsuarioForum usuario, boolean administrador) {
        String email = administrador ? VisibilidadeEmail.mascarar(usuario.getEmail()) : null;
        return new UsuarioForumResponse(usuario.getId(), usuario.getNome(), email, usuario.getPerfil());
    }

}
