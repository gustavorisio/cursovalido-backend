package com.cursovalido.backend.aulas.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tb_aulas")
public class Aula {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 255)
    private String titulo;
    @Column(nullable = false, length = 5000)
    private String descricao;
    @Column(nullable = false)
    private boolean ativo = true;
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
    @Column(name = "modulo_id", nullable = false)
    private Long idModulo;
    @Column(nullable = false)
    private int ordem;
    @Column(name = "youtube_video_id", nullable = false, length = 11)
    private String youtubeVideoId;
}
