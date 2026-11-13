package com.cursovalido.backend.aulas.repository;

import com.cursovalido.backend.aulas.entity.Tema;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemaRepository extends JpaRepository<Tema, Long> {
    List<Tema> findAllByOrderByNomeAsc();
    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
