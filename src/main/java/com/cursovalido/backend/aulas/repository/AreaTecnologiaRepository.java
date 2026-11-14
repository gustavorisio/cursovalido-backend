package com.cursovalido.backend.aulas.repository;

import com.cursovalido.backend.aulas.entity.AreaTecnologia;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaTecnologiaRepository extends JpaRepository<AreaTecnologia, Long> {
    List<AreaTecnologia> findAllByOrderByOrdemAsc();
}
