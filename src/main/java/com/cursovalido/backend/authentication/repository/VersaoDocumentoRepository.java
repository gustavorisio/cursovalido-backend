package com.cursovalido.backend.authentication.repository;

import com.cursovalido.backend.authentication.entity.VersaoDocumento;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface VersaoDocumentoRepository extends JpaRepository<VersaoDocumento, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from VersaoDocumento v where v.tipo = :tipo")
    Optional<VersaoDocumento> bloquear(@Param("tipo") String tipo);
}
