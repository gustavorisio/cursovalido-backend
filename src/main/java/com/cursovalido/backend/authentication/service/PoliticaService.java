package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.dto.PoliticaRequest;
import com.cursovalido.backend.authentication.dto.PoliticaResponse;
import com.cursovalido.backend.authentication.entity.Politica;
import com.cursovalido.backend.authentication.repository.PoliticaRepository;
import com.cursovalido.backend.authentication.validation.administracao.PrivilegiosAdministrativosService;
import com.cursovalido.backend.authentication.validation.politica.ConteudoDocumento;
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
    private final VersaoDocumentoService versoes;

    // Lista as políticas que estão ativas no momento.
    public List<PoliticaResponse> listarAtivas() {
        return repositorio.findByAtivaTrueOrderByDataPublicacaoDesc().stream()
                .map(politica -> new PoliticaResponse(politica.getNome(), politica.getVersao(), politica.getConteudo(),
                        politica.getDataPublicacao()))
                .toList();
    }

    // Verifica se uma versão de política esta ativa.
    public boolean versaoAtiva(String versao) {
        return repositorio.existsByAtivaTrueAndVersao(versao);
    }

    @Transactional
    // Publica uma nova versão e pede novo aceite aos usuários.
    public PoliticaResponse publicar(PoliticaRequest requisicao, Long administradorId) {
        privilegios.verificarPrincipal(administradorId);
        String conteudo = ConteudoDocumento.preparar(requisicao.conteudo());
        String versao = versoes.proxima("TERMOS");

        repositorio.findByAtivaTrueOrderByDataPublicacaoDesc().forEach(politica -> {
            politica.setAtiva(false);
            repositorio.save(politica);
        });

        Politica publicada = repositorio.save(new Politica(requisicao.nome().trim(),
                versao, conteudo));
        auditoria.registrar(administradorId, "POLITICA_PUBLICADA", publicada.getVersao(), null);
        return converter(publicada);
    }

    @Transactional(readOnly = true)
    public List<PoliticaResponse> listarHistorico() {
        return repositorio.findByAtivaFalseOrderByDataPublicacaoDescIdDesc().stream()
                .map(this::converter).toList();
    }

    private PoliticaResponse converter(Politica politica) {
        return new PoliticaResponse(politica.getNome(), politica.getVersao(), politica.getConteudo(),
                politica.getDataPublicacao());
    }
}
