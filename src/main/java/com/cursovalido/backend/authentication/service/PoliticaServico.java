package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.dto.PoliticaResposta;
import com.cursovalido.backend.authentication.dto.PoliticaRequisicao;
import com.cursovalido.backend.authentication.entity.AceiteTermoArquivado;
import com.cursovalido.backend.authentication.entity.Politica;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.AceiteTermoArquivadoRepositorio;
import com.cursovalido.backend.authentication.repository.PoliticaRepositorio;
import com.cursovalido.backend.authentication.repository.UsuarioRepositorio;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class PoliticaServico {
    @Autowired
    private PoliticaRepositorio repositorio;
    @Autowired
    private UsuarioRepositorio usuarios;
    @Autowired
    private AceiteTermoArquivadoRepositorio aceitesArquivados;
    @Autowired
    private LogAuditoriaServico auditoria;

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
        if (repositorio.findByVersao(requisicao.versao()).isPresent()) {
            throw new IllegalArgumentException("A versao informada ja existe");
        }

        String versaoAnterior = repositorio.findByAtivaTrueOrderByDataPublicacaoDesc().stream()
                .map(Politica::getVersao)
                .findFirst()
                .orElse(null);
        repositorio.findByAtivaTrueOrderByDataPublicacaoDesc()
                .forEach(politica -> {
                    politica.setAtiva(false);
                    repositorio.save(politica);
                });

        if (versaoAnterior != null) {
            for (Usuario usuario : usuarios.findAll()) {
                if (Boolean.TRUE.equals(usuario.getAceitouTermosLgpd())
                        && versaoAnterior.equals(usuario.getVersaoTermos())) {
                    aceitesArquivados.save(new AceiteTermoArquivado(usuario.getId(),
                            usuario.getVersaoTermos(), usuario.getDataHoraAceite(), "NOVA_VERSAO_TERMOS"));
                    usuario.setAceitouTermosLgpd(false);
                    usuarios.save(usuario);
                }
            }
        }

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