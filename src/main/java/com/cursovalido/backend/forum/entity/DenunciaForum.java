package com.cursovalido.backend.forum.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_denuncias_forum")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DenunciaForum {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoConteudoForum tipo;

    @Column(name = "conteudo_id", nullable = false)
    private Long idConteudo;

    @Column(name = "autor_conteudo_id", nullable = false)
    private Long idAutorConteudo;

    @Column(name = "denunciante_id", nullable = false)
    private Long idDenunciante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MotivoDenuncia motivo;

    @Column(length = 1000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusDenuncia status;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @Column(name = "moderador_id")
    private Long idModerador;

    @Column(name = "decidida_em")
    private LocalDateTime decididaEm;

    @Column(name = "observacao_moderacao", length = 1000)
    private String observacaoModeracao;

    @Column(name = "conteudo_removido", nullable = false)
    private boolean conteudoRemovido;

    public DenunciaForum(TipoConteudoForum tipo, Long idConteudo, Long idAutorConteudo, Long idDenunciante,
            MotivoDenuncia motivo, String descricao) {
        this.tipo = tipo;
        this.idConteudo = idConteudo;
        this.idAutorConteudo = idAutorConteudo;
        this.idDenunciante = idDenunciante;
        this.motivo = motivo;
        this.descricao = descricao;
        this.status = StatusDenuncia.PENDENTE;
        this.criadaEm = LocalDateTime.now();
    }

    public void decidir(boolean procedente, boolean removido, String observacao, Long moderador) {
        this.status = procedente ? StatusDenuncia.PROCEDENTE : StatusDenuncia.IMPROCEDENTE;
        this.conteudoRemovido = removido;
        this.observacaoModeracao = observacao;
        this.idModerador = moderador;
        this.decididaEm = LocalDateTime.now();
    }
}
