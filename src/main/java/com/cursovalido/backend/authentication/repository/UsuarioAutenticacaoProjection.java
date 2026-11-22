package com.cursovalido.backend.authentication.repository;

public interface UsuarioAutenticacaoProjection {
    Long getId();

    String getEmail();

    String getSenha();

    String getPerfil();

    Boolean getEmailConfirmado();

    Boolean getAtivo();

    Boolean getTermosAceitos();

    Boolean getVersaoTermosAtiva();

    Boolean getAdministradorPrincipal();

    String getVersaoTermos();
}
