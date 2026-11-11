package com.cursovalido.backend.authentication.controller;

import com.cursovalido.backend.authentication.dto.AceiteTermosRequest;
import com.cursovalido.backend.authentication.dto.AlteracaoSenhaRequest;
import com.cursovalido.backend.authentication.dto.AutenticacaoResponse;
import com.cursovalido.backend.authentication.dto.CadastroUsuarioRequest;
import com.cursovalido.backend.authentication.dto.Codigo2faRequest;
import com.cursovalido.backend.authentication.dto.DadosPessoaisResponse;
import com.cursovalido.backend.authentication.dto.EmailRequest;
import com.cursovalido.backend.authentication.dto.LoginRequest;
import com.cursovalido.backend.authentication.dto.TokenEmailRequest;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import com.cursovalido.backend.authentication.service.AutenticacaoService;
import com.cursovalido.backend.authentication.service.CadastroUsuarioService;
import com.cursovalido.backend.authentication.service.ContaUsuarioService;
import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import com.cursovalido.backend.authentication.service.SenhaUsuarioService;
import com.cursovalido.backend.authentication.service.TermosUsuarioService;
import com.cursovalido.backend.authentication.service.TokenJwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/autenticacao")
@RequiredArgsConstructor
public class AutenticacaoController {
    private final CadastroUsuarioService cadastroUsuarios;
    private final SenhaUsuarioService senhasUsuarios;
    private final TermosUsuarioService termosUsuarios;
    private final ContaUsuarioService contaUsuarios;
    private final AutenticacaoService autenticacao;
    private final TokenJwtService tokens;
    private final LogAuditoriaService auditoria;

    @GetMapping("/meus-dados")
    public DadosPessoaisResponse meusDados(
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return contaUsuarios.consultarDados(usuario.getId());
    }

    @GetMapping("/exportar-dados")
    public ResponseEntity<DadosPessoaisResponse> exportarDados(
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        var dados = contaUsuarios.consultarDadosParaExportacao(usuario.getId());
        auditoria.registrar(usuario.getId(), "EXPORTACAO_DADOS", "Dados de cadastro exportados", null);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=meus-dados.json")
                .header("Cache-Control", "no-store")
                .body(dados);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader("Authorization") String autorizacao,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        tokens.encerrar(autorizacao.substring(7));
        auditoria.registrar(usuario.getId(), "LOGOUT", "Sessao encerrada", null);
    }

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public void cadastrar(@Valid @RequestBody CadastroUsuarioRequest requisicao, HttpServletRequest request) {
        cadastroUsuarios.cadastrar(requisicao, request.getRemoteAddr());
    }

    @PostMapping("/login")
    public AutenticacaoResponse login(@Valid @RequestBody LoginRequest requisicao, HttpServletRequest request) {
        return autenticacao.autenticar(requisicao, request.getRemoteAddr());
    }

    @PostMapping("/confirmar-2fa")
    public AutenticacaoResponse confirmar2fa(@Valid @RequestBody Codigo2faRequest requisicao,
            HttpServletRequest request) {
        return autenticacao.confirmarCodigo(requisicao.email(), requisicao.codigo(), request.getRemoteAddr());
    }

    @PostMapping("/reenviar-2fa")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reenviar2fa(@Valid @RequestBody EmailRequest requisicao, HttpServletRequest request) {
        autenticacao.reenviarCodigo2fa(requisicao.email(), request.getRemoteAddr());
    }

    @PostMapping("/confirmar-email")
    public void confirmarEmail(@Valid @RequestBody TokenEmailRequest requisicao) {
        cadastroUsuarios.confirmarEmail(requisicao.token());
    }

    @PostMapping("/reenviar-confirmacao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reenviarConfirmacao(@Valid @RequestBody EmailRequest requisicao) {
        cadastroUsuarios.reenviarConfirmacao(requisicao.email());
    }

    @PostMapping("/solicitar-alteracao-senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void solicitarAlteracaoSenha(@Valid @RequestBody EmailRequest requisicao) {
        senhasUsuarios.solicitarAlteracaoSenha(requisicao.email());
    }

    @PostMapping("/alterar-senha")
    public void alterarSenha(@Valid @RequestBody AlteracaoSenhaRequest requisicao) {
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
            @RequestHeader("Authorization") String autorizacao,
            @Valid @RequestBody AceiteTermosRequest requisicao) {
        termosUsuarios.aceitarTermosNovamente(usuario.getId(), requisicao.versaoTermos());
        tokens.encerrar(autorizacao.substring(7));
    }

    @PostMapping("/conta/solicitar-exclusao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void solicitarExclusao(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        contaUsuarios.solicitarExclusao(usuario.getId());
    }

    @DeleteMapping("/conta")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirConta(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody com.cursovalido.backend.authentication.dto.ExclusaoContaRequest requisicao) {
        contaUsuarios.excluirConta(usuario.getId(), requisicao.codigo());
    }
}
