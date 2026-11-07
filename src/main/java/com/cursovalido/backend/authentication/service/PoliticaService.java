package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.dto.PoliticaRequisicao;
import com.cursovalido.backend.authentication.dto.PoliticaResposta;

import com.cursovalido.backend.authentication.entity.Politica;


import com.cursovalido.backend.authentication.repository.PoliticaRepository;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PoliticaService {
    private final PoliticaRepository repositorio;


    private final LogAuditoriaService auditoria;
    private final PrivilegiosAdministrativosService privilegios;

    // Lista as politicas que estao ativas no momento.
    public List<PoliticaResposta> listarAtivas() {
        return repositorio.findByAtivaTrueOrderByDataPublicacaoDesc().stream()
                .map(politica -> new PoliticaResposta(politica.getNome(), politica.getVersao(), politica.getConteudo(),
                        politica.getDataPublicacao()))
                .toList();
    }

    // Verifica se uma versao de politica esta ativa.
    public boolean versaoAtiva(String versao) {
        return repositorio.findByAtivaTrueOrderByDataPublicacaoDesc().stream()
                .anyMatch(politica -> politica.getVersao().equals(versao));
    }

    @Transactional
    // Publica uma nova versao e pede novo aceite aos usuarios.
    public PoliticaResposta publicar(PoliticaRequisicao requisicao, Long administradorId) {
        privilegios.verificarPrincipal(administradorId);
        if (repositorio.findByVersao(requisicao.versao()).isPresent()) {
            throw new IllegalArgumentException("A versao informada ja existe");
        }

        repositorio.findByAtivaTrueOrderByDataPublicacaoDesc().forEach(politica -> {
            politica.setAtiva(false);
            repositorio.save(politica);
        });

        Politica publicada = repositorio.save(new Politica(requisicao.nome().trim(),
                requisicao.versao().trim(), requisicao.conteudo().trim()));
        auditoria.registrar(administradorId, "POLITICA_PUBLICADA", publicada.getVersao(), null);
        return converter(publicada);
    }

    private PoliticaResposta converter(Politica politica) {
        return new PoliticaResposta(politica.getNome(), politica.getVersao(), politica.getConteudo(),
                politica.getDataPublicacao());
    }
}
