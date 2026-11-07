package com.cursovalido.backend.address.service;

import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import com.cursovalido.backend.address.dto.EnderecoResponse;
import com.cursovalido.backend.address.exception.CepInvalidoException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class EnderecoService {
    @Autowired
    private LogAuditoriaService auditoria;

    // Consulta os dados do endereco pelo CEP informado.
    public EnderecoResponse consultar(String cep, String enderecoIp) {
        String cepLimpo = cep.replaceAll("\\D", "");
        auditoria.registrar(null, "CONSULTA_CEP", cepLimpo, enderecoIp);
        if (!cepLimpo.matches("\\d{8}"))
            throw new CepInvalidoException();
        try {
            RestTemplate cliente = new RestTemplate();
            EnderecoResponse resposta = cliente.getForObject(
                    "https://viacep.com.br/ws/{cep}/json/", EnderecoResponse.class, cepLimpo);
            if (resposta == null || Boolean.TRUE.equals(resposta.erro()))
                throw new CepInvalidoException();
            return resposta;
        } catch (RuntimeException excecao) {
            excecao.printStackTrace();
            throw new CepInvalidoException();
        }
    }
}