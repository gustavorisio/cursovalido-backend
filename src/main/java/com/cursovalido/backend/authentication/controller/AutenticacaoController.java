package com.cursovalido.backend.authentication.controller;

import com.cursovalido.backend.authentication.dto.AceiteTermosRequisicao;
import com.cursovalido.backend.authentication.dto.AlteracaoSenhaRequisicao;
import com.cursovalido.backend.authentication.dto.AutenticacaoResposta;
import com.cursovalido.backend.authentication.dto.CadastroUsuarioRequisicao;
import com.cursovalido.backend.authentication.dto.Codigo2faRequisicao;
import com.cursovalido.backend.authentication.dto.DadosPessoaisResposta;
import com.cursovalido.backend.authentication.dto.EmailRequisicao;
import com.cursovalido.backend.authentication.dto.LoginRequisicao;
import com.cursovalido.backend.authentication.dto.TokenEmailRequisicao;
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
    public DadosPessoaisResposta meusDados(
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return contaUsuarios.consultarDados(usuario.getId());
    }

    @GetMapping("/exportar-dados")
    public ResponseEntity<DadosPessoaisResposta> exportarDados(
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        var dados = contaUsuarios.consultarDados(usuario.getId());
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
            @RequestHeader("Authorization") String autorizacao,
            @Valid @RequestBody AceiteTermosRequisicao requisicao) {
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
            @Valid @RequestBody com.cursovalido.backend.authentication.dto.ExclusaoContaRequisicao requisicao) {
        contaUsuarios.excluirConta(usuario.getId(), requisicao.codigo());
    }
}
