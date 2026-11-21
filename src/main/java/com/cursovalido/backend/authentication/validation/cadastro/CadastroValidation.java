package com.cursovalido.backend.authentication.validation.cadastro;

import com.cursovalido.backend.authentication.dto.CadastroUsuarioRequest;
import com.cursovalido.backend.authentication.exception.CadastroDuplicadoException;
import com.cursovalido.backend.authentication.exception.TermosNaoDisponiveisException;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import com.cursovalido.backend.authentication.service.PoliticaService;

public final class CadastroValidation {
    private CadastroValidation() {
    }

    public static void validar(CadastroUsuarioRequest requisicao, String enderecoIp, PoliticaService politicas,
            UsuarioRepository repositorio, LogAuditoriaService auditoria) {
        validarDados(requisicao, enderecoIp, politicas, repositorio, auditoria);
        if (repositorio.existsByEmail(requisicao.email()))
            throw new CadastroDuplicadoException("E-mail");
    }

    public static void validarDados(CadastroUsuarioRequest requisicao, String enderecoIp, PoliticaService politicas,
            UsuarioRepository repositorio, LogAuditoriaService auditoria) {
        if (!politicas.versaoAtiva(requisicao.versaoTermos()))
            throw new TermosNaoDisponiveisException();
        if (!Boolean.TRUE.equals(requisicao.declarouMaiorIdade()))
            throw new IllegalArgumentException("É necessário confirmar que você tem 18 anos ou mais.");
    }
}
