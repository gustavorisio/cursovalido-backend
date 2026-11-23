package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.dto.TermoResponsabilidadeResponse;
import com.cursovalido.backend.authentication.entity.*;
import com.cursovalido.backend.authentication.repository.AceiteResponsabilidadeRepository;
import com.cursovalido.backend.authentication.validation.responsabilidade.ResponsabilidadeValidation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResponsabilidadeService {
    private final AceiteResponsabilidadeRepository aceites;
    private final LogAuditoriaService auditoria;

    // Cada alteração de conteúdo exige uma nova versão e novo aceite.
    public TermoResponsabilidadeResponse documento(Perfil perfil) {
        String conteudo = switch (perfil) {
            case PROFESSOR -> "Ao aceitar, declaro possuir os direitos e autorizacoes sobre os conteudos que publico, "
                    + "incluindo aulas, links e forum. Autorizo o Curso Valido a reproduzir, disponibilizar e manter "
                    + "esses conteudos para fins educacionais, inclusive apos o encerramento da conta, respeitados "
                    + "os direitos autorais de terceiros. Comprometo-me a indicar as fontes, publicar somente "
                    + "materiais autorizados e responder por violacoes decorrentes das minhas publicacoes. "
                    + "Manterei conduta respeitosa, sem assedio ou discriminacao, e sou responsavel pelas minhas "
                    + "interacoes com alunos e pela protecao dos dados pessoais a que tiver acesso.";
            case ADMINISTRADOR -> "Comprometo-me a manter confidencialidade sobre dados pessoais e informacoes "
                    + "restritas, inclusive apos encerrar minha conta. Usarei privilegios elevados apenas para "
                    + "finalidades autorizadas da plataforma, sem compartilhar credenciais ou contornar controles. "
                    + "No tratamento de dados de terceiros, observarei a LGPD, a finalidade, a necessidade e as "
                    + "permissoes de acesso. Sou responsavel pelas operacoes administrativas que realizar, pela "
                    + "guarda das minhas credenciais e pela comunicacao de incidentes a administracao.";
            case ALUNO -> throw new IllegalArgumentException("Este perfil não possui termo de responsabilidade.");
        };
        return new TermoResponsabilidadeResponse(perfil, "1.0", conteudo);
    }

    public boolean atualizado(Usuario usuario) {
        return !usuario.getPerfil().exigeTermoResponsabilidade() || aceites.existsByUsuarioIdAndPerfilAndVersao(
                usuario.getId(), usuario.getPerfil(), documento(usuario.getPerfil()).versao());
    }

    public void validar(Perfil perfil, Boolean aceitou, String versao, String ip) {
        ResponsabilidadeValidation.validar(perfil, aceitou, versao, ip, this);
    }

    @Transactional
    public void registrar(Usuario usuario, Boolean aceitou, String versao, String ip) {
        validar(usuario.getPerfil(), aceitou, versao, ip);
        if (!usuario.getPerfil().exigeTermoResponsabilidade())
            return;
        aceites.save(new AceiteResponsabilidade(usuario.getId(), usuario.getPerfil(), versao, ip));
        auditoria.registrar(usuario.getId(), "ACEITE_RESPONSABILIDADE",
                "Perfil " + usuario.getPerfil() + "; termo_version=" + versao, ip);
    }
}
