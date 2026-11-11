package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.dto.PoliticaRequest;
import com.cursovalido.backend.authentication.dto.PoliticaResponse;
import com.cursovalido.backend.authentication.entity.PoliticaPrivacidade;
import com.cursovalido.backend.authentication.repository.PrivacidadeRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PrivacidadeService {
    private final PrivacidadeRepository repositorio;
    private final PrivilegiosAdministrativosService privilegios;
    private final LogAuditoriaService auditoria;

    public PoliticaResponse buscarAtiva() {
        return repositorio.findByAtivaTrueOrderByDataPublicacaoDesc().stream()
                .findFirst().map(this::converter).orElse(null);
    }

    public boolean atualizadaDesde(LocalDateTime aceite) {
        var atual = buscarAtiva();
        if (atual == null) {
            return false;
        }
        if (aceite == null) {
            return true;
        }
        return atual.dataPublicacao().isAfter(aceite);
    }

    @Transactional
    public PoliticaResponse publicar(PoliticaRequest dados, Long administradorId) {
        privilegios.verificarPrincipal(administradorId);
        if (repositorio.findByVersao(dados.versao().trim()).isPresent())
            throw new IllegalArgumentException("A versao informada ja existe");
        repositorio.findByAtivaTrueOrderByDataPublicacaoDesc().forEach(politica -> {
            politica.setAtiva(false);
            repositorio.save(politica);
        });
        var publicada = repositorio
                .save(new PoliticaPrivacidade(dados.nome().trim(), dados.versao().trim(), dados.conteudo().trim()));
        auditoria.registrar(administradorId, "PRIVACIDADE_PUBLICADA", publicada.getVersao(), null);
        return converter(publicada);
    }

    private PoliticaResponse converter(PoliticaPrivacidade politica) {
        return new PoliticaResponse(politica.getNome(), politica.getVersao(), politica.getConteudo(),
                politica.getDataPublicacao());
    }
}
