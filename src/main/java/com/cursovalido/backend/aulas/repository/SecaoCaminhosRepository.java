package com.cursovalido.backend.aulas.repository;

import com.cursovalido.backend.aulas.entity.SecaoCaminhos;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecaoCaminhosRepository extends JpaRepository<SecaoCaminhos, Long> {
    List<SecaoCaminhos> findAllByOrderByOrdemAscIdAsc();
    List<SecaoCaminhos> findByPublicadoTrueOrderByOrdemAscIdAsc();
}
