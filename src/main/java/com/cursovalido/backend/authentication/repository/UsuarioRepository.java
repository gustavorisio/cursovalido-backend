package com.cursovalido.backend.authentication.repository;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.Usuario;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    @Query(value = """
            SELECT u.id AS id,
                   u.email AS email,
                   u.senha AS senha,
                   u.perfil AS perfil,
                   u.email_confirmado AS "emailConfirmado",
                   (u.ativo AND u.status_aprovacao = 'APROVADO') AS ativo,
                   (u.aceitou_termos_lgpd AND (
                       u.perfil = 'ALUNO' OR EXISTS (
                           SELECT 1 FROM tb_aceites_responsabilidade a
                           WHERE a.user_id = u.id AND a.perfil = u.perfil
                             AND a.termo_version = :versaoResponsabilidade
                       )
                   )) AS "termosAceitos",
                   EXISTS (
                       SELECT 1 FROM tb_politicas p
                       WHERE p.ativa AND p.versao = u.versao_termos
                   ) AS "versaoTermosAtiva",
                   EXISTS (
                       SELECT 1 FROM tb_inicializacao_administrador i
                       WHERE i.id = 1 AND i.id_usuario = u.id
                   ) AS "administradorPrincipal",
                   u.versao_termos AS "versaoTermos"
            FROM tb_usuarios u
            WHERE u.id = :idUsuario AND u.email = :email
              AND EXISTS (
                  SELECT 1 FROM tb_sessoes s
                  WHERE s.id = :idSessao AND s.usuario_id = u.id
                    AND s.revogada = FALSE AND s.expira_em > :agora
              )
            """, nativeQuery = true)
    Optional<UsuarioAutenticacaoProjection> carregarAutenticacao(
            @Param("idUsuario") Long idUsuario,
            @Param("email") String email,
            @Param("idSessao") String idSessao,
            @Param("agora") LocalDateTime agora,
            @Param("versaoResponsabilidade") String versaoResponsabilidade);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> buscarParaAtualizar(@Param("id") Long id);

    List<Usuario> findByExcluidoEmIsNull();

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPerfil(Perfil perfil);

    boolean existsByMatricula(String matricula);

    void deleteById(Long id);
}
