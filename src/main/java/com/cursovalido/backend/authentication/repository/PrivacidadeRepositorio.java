package com.cursovalido.backend.authentication.repository;
import com.cursovalido.backend.authentication.entity.PoliticaPrivacidade;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PrivacidadeRepositorio extends JpaRepository<PoliticaPrivacidade, Long> {
    List<PoliticaPrivacidade> findByAtivaTrueOrderByDataPublicacaoDesc();
    Optional<PoliticaPrivacidade> findByVersao(String versao);
}
