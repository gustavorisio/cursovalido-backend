package com.cursovalido.backend.authentication.validation.cadastro;

import com.cursovalido.backend.authentication.dto.CadastroUsuarioRequest;
import com.cursovalido.backend.authentication.exception.CadastroDuplicadoException;
import com.cursovalido.backend.authentication.exception.LimiteReenvioException;
import com.cursovalido.backend.authentication.exception.MenorDeIdadeException;
import com.cursovalido.backend.authentication.exception.TermosNaoDisponiveisException;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import com.cursovalido.backend.authentication.service.PoliticaService;
import java.time.LocalDate;
import java.time.Period;

public final class CadastroValidation {
    private CadastroValidation() {
    }

    public static void validar(CadastroUsuarioRequest requisicao, String enderecoIp, PoliticaService politicas,
            UsuarioRepository repositorio, LogAuditoriaService auditoria) {
        if (!politicas.versaoAtiva(requisicao.versaoTermos()))
            throw new TermosNaoDisponiveisException();
        if (Period.between(requisicao.dataNascimento(), LocalDate.now()).getYears() < 18) {
            auditoria.registrar(null, "TENTATIVA_CADASTRO_MENOR_IDADE", "Maioridade nao atendida", enderecoIp);
            throw new MenorDeIdadeException();
        }
        if (Period.between(requisicao.dataNascimento(), LocalDate.now()).getYears() > 90)
            throw new IllegalArgumentException("A idade maxima permitida e 90 anos.");
        if (repositorio.existsByEmail(requisicao.email()))
            throw new CadastroDuplicadoException("E-mail");
    }
    public static void validarReenvio(boolean limiteExcedido) {
        if (limiteExcedido)
            throw new LimiteReenvioException();
    }
}
