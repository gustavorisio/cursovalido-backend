package com.cursovalido.backend.authentication.repository;

import com.cursovalido.backend.authentication.entity.LogAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.List;

public interface LogAuditoriaRepositorio extends JpaRepository<LogAuditoria, Long> {
	Optional<LogAuditoria> findTopByOrderByIdDesc();

	List<LogAuditoria> findAllByOrderByIdAsc();

	@Modifying
	@Transactional
	@Query("update LogAuditoria log set log.usuarioId = null where log.usuarioId = :usuarioId")
	void anonimizarPorUsuarioId(@Param("usuarioId") Long usuarioId);
}