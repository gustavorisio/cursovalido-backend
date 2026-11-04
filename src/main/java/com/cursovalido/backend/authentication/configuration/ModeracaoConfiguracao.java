package com.cursovalido.backend.authentication.configuration;

import com.cursovalido.backend.authentication.entity.EscopoSuspensao;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import com.cursovalido.backend.authentication.service.ModeracaoUsuarioServico;
import com.cursovalido.backend.authentication.service.PrivilegiosAdministrativosServico;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class ModeracaoConfiguracao implements WebMvcConfigurer {
    private final ModeracaoUsuarioServico moderacao;
    private final PrivilegiosAdministrativosServico privilegios;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                var autenticacao = SecurityContextHolder.getContext().getAuthentication();
                if (autenticacao == null) return true;
                if (!(autenticacao.getPrincipal() instanceof UsuarioAutenticado usuario)) return true;
                String caminho = request.getRequestURI().substring(request.getContextPath().length());
                if (caminho.equals("/api/autenticacao/logout")) return true;
                if (caminho.equals("/api/autenticacao/restricoes")) return true;
                if (moderacao.restrito(usuario.getId(), EscopoSuspensao.CONTA)) {
                    return bloquear(response, "CONTA_SUSPENSA", moderacao.mensagemBanimento(usuario.getId(), EscopoSuspensao.CONTA));
                }
                if (caminho.startsWith("/api/administracao/convites")) privilegios.verificarPrincipal(usuario.getId());
                if (caminho.equals("/api/politicas")) {
                    if (request.getMethod().equals("POST")) privilegios.verificarPrincipal(usuario.getId());
                }
                if (!caminho.startsWith("/api/forum/")) return true;
                if (moderacao.restrito(usuario.getId(), EscopoSuspensao.FORUM)) {
                    return bloquear(response, "FORUM_SUSPENSO", moderacao.mensagemBanimento(usuario.getId(), EscopoSuspensao.FORUM));
                }
                boolean conteudoForum = caminho.equals("/api/forum/topicos")
                        || caminho.startsWith("/api/forum/topicos/")
                        || caminho.startsWith("/api/forum/comentarios/");
                if (conteudoForum) {
                    if (moderacao.restrito(usuario.getId(), EscopoSuspensao.TOPICOS)) {
                        return bloquear(response, "TOPICOS_SUSPENSOS", moderacao.mensagemBanimento(usuario.getId(), EscopoSuspensao.TOPICOS));
                    }
                    if (moderacao.restrito(usuario.getId(), EscopoSuspensao.COMENTARIOS)) {
                        return bloquear(response, "COMENTARIOS_SUSPENSOS", moderacao.mensagemBanimento(usuario.getId(), EscopoSuspensao.TOPICOS));
                    }
                }
                return true;
            }
        }).addPathPatterns("/api/**");
    }

    private boolean bloquear(HttpServletResponse response, String codigo, String mensagem) throws Exception {
        response.setStatus(403);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"codigo\":\"" + codigo + "\",\"mensagem\":\"" + mensagem + "\"}");
        return false;
    }
}
