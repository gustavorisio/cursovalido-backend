package com.cursovalido.backend.authentication.repository;

import com.cursovalido.backend.authentication.entity.AceiteTermoArquivado;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AceiteTermoArquivadoRepository extends JpaRepository<AceiteTermoArquivado, Long> {
    List<AceiteTermoArquivado> findByUsuarioIdOriginalOrderByDataHoraArquivamentoAsc(Long usuarioId);
}
