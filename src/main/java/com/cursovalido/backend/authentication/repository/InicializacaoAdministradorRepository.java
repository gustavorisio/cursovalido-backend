package com.cursovalido.backend.authentication.repository;

import com.cursovalido.backend.authentication.entity.InicializacaoAdministrador;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface InicializacaoAdministradorRepository extends JpaRepository<InicializacaoAdministrador, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InicializacaoAdministrador i where i.id = 1")
    Optional<InicializacaoAdministrador> buscarParaAtualizar();
}
