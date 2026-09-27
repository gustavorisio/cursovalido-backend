package com.cursovalido.backend.authentication.controller;

import com.cursovalido.backend.authentication.dto.AutenticacaoResposta;
import com.cursovalido.backend.authentication.dto.CadastroUsuarioRequisicao;
import com.cursovalido.backend.authentication.dto.LoginRequisicao;
import com.cursovalido.backend.authentication.dto.TokenEmailRequisicao;
import com.cursovalido.backend.authentication.dto.EmailRequisicao;
import com.cursovalido.backend.authentication.dto.AlteracaoSenhaRequisicao;
import com.cursovalido.backend.authentication.dto.AceiteTermosRequisicao;
import com.cursovalido.backend.authentication.dto.Codigo2faRequisicao;
import com.cursovalido.backend.authentication.service.AutenticacaoServico;
import com.cursovalido.backend.authentication.service.CadastroUsuarioServico;
import com.cursovalido.backend.authentication.service.ContaUsuarioServico;
import com.cursovalido.backend.authentication.service.SenhaUsuarioServico;
import com.cursovalido.backend.authentication.service.TermosUsuarioServico;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/autenticacao")
public class AutenticacaoControlador {
    @Autowired
    private CadastroUsuarioServico cadastroUsuarios;
    @Autowired
    private SenhaUsuarioServico senhasUsuarios;
    @Autowired
    private TermosUsuarioServico termosUsuarios;
    @Autowired
    private ContaUsuarioServico contaUsuarios;
    @Autowired
    private AutenticacaoServico autenticacao;

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public void cadastrar(@Valid @RequestBody CadastroUsuarioRequisicao requisicao, HttpServletRequest request) {
        cadastroUsuarios.cadastrar(requisicao, request.getRemoteAddr());
    }

    @PostMapping("/login")
    public AutenticacaoResposta login(@Valid @RequestBody LoginRequisicao requisicao, HttpServletRequest request) {
        return autenticacao.autenticar(requisicao, request.getRemoteAddr());
    }

    @PostMapping("/confirmar-2fa")
    public AutenticacaoResposta confirmar2fa(@Valid @RequestBody Codigo2faRequisicao requisicao,
            HttpServletRequest request) {
        return autenticacao.confirmarCodigo(requisicao.email(), requisicao.codigo(), request.getRemoteAddr());
    }

    @PostMapping("/reenviar-2fa")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reenviar2fa(@Valid @RequestBody EmailRequisicao requisicao, HttpServletRequest request) {
        autenticacao.reenviarCodigo2fa(requisicao.email(), request.getRemoteAddr());
    }

    @PostMapping("/confirmar-email")
    public void confirmarEmail(@Valid @RequestBody TokenEmailRequisicao requisicao) {
        cadastroUsuarios.confirmarEmail(requisicao.token());
    }

    @PostMapping("/reenviar-confirmacao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reenviarConfirmacao(@Valid @RequestBody EmailRequisicao requisicao) {
        cadastroUsuarios.reenviarConfirmacao(requisicao.email());
    }

    @PostMapping("/solicitar-alteracao-senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void solicitarAlteracaoSenha(@Valid @RequestBody EmailRequisicao requisicao) {
        senhasUsuarios.solicitarAlteracaoSenha(requisicao.email());
    }

    @PostMapping("/alterar-senha")
    public void alterarSenha(@Valid @RequestBody AlteracaoSenhaRequisicao requisicao) {
        senhasUsuarios.alterarSenha(requisicao.token(), requisicao.novaSenha(), requisicao.confirmacaoSenha());
    }

    @PostMapping("/revogar-termos")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revogarTermos(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        termosUsuarios.revogarTermos(usuario.getId());
    }

    @PostMapping("/aceitar-termos-novamente")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void aceitarTermosNovamente(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody AceiteTermosRequisicao requisicao) {
        termosUsuarios.aceitarTermosNovamente(usuario.getId(), requisicao.versaoTermos());
    }

    @DeleteMapping("/conta")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirConta(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        contaUsuarios.excluirConta(usuario.getId());
    }
}