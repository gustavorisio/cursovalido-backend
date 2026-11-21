package com.cursovalido.backend.forum.repository;

import com.cursovalido.backend.forum.entity.DenunciaForum;
import com.cursovalido.backend.forum.entity.StatusDenuncia;
import com.cursovalido.backend.forum.entity.TipoConteudoForum;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DenunciaForumRepository extends JpaRepository<DenunciaForum, Long> {
    boolean existsByIdDenuncianteAndIdAutorConteudoAndCriadaEmAfter(Long idDenunciante, Long idAutorConteudo,
            LocalDateTime desde);

    List<DenunciaForum> findByStatusOrderByCriadaEmAscIdAsc(StatusDenuncia status);

    List<DenunciaForum> findByTipoAndIdConteudoAndStatus(TipoConteudoForum tipo, Long idConteudo,
            StatusDenuncia status);

    long countByTipoAndIdConteudoAndStatus(TipoConteudoForum tipo, Long idConteudo, StatusDenuncia status);
}
