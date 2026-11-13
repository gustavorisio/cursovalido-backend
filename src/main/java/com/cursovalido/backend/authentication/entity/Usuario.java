package com.cursovalido.backend.authentication.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_usuarios")
@Getter
@NoArgsConstructor
@Setter
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;
    @Column(name = "nome_completo", nullable = false, length = 120)
    private String nomeCompleto;
    @Column(nullable = false, unique = true, length = 254)
    private String email;
    @Column(nullable = false, length = 60)
    private String senha;
    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;
    @Column(nullable = false, unique = true, length = 50)
    private String matricula;
    @Column(name = "declarou_maior_idade", nullable = false)
    private Boolean declarouMaiorIdade;
    @Column(name = "aceitou_termos_lgpd", nullable = false)
    private Boolean aceitouTermosLgpd;
    @Column(name = "data_hora_aceite")
    private LocalDateTime dataHoraAceite;
    @Column(name = "versao_termos", length = 30)
    private String versaoTermos;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Perfil perfil;
    @Column(nullable = false)
    private boolean ativo = true;
    @Column(name = "email_confirmado", nullable = false)
    private boolean emailConfirmado = false;
    @Enumerated(EnumType.STRING)
    @Column(name = "status_aprovacao", nullable = false)
    private StatusAprovacao statusAprovacao = StatusAprovacao.APROVADO;
}
