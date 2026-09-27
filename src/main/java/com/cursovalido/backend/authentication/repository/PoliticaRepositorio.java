package com.cursovalido.backend.authentication.repository;

import com.cursovalido.backend.authentication.entity.Politica;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoliticaRepositorio extends JpaRepository<Politica, Long> {
    List<Politica> findByAtivaTrueOrderByDataPublicacaoDesc();

    Optional<Politica> findByVersao(String versao);
}