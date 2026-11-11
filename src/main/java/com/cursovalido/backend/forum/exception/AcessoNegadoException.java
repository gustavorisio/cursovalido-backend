package com.cursovalido.backend.forum.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class AcessoNegadoException extends ResponseStatusException {
    public static final String AUTOR_EDITAR_TOPICO = "Somente o autor pode editar o topico";
    public static final String AUTOR_OU_ADMIN_ALTERAR_TOPICO = "Somente o autor ou administrador pode alterar o topico";
    public static final String ADMIN_APAGAR_TOPICO = "Somente o administrador pode apagar o topico";
    public static final String AUTOR_EDITAR_COMENTARIO = "Somente o autor pode editar o comentario";
    public static final String AUTOR_OU_ADMIN_APAGAR_COMENTARIO = "Somente o autor ou administrador pode apagar";

    public AcessoNegadoException(String mensagem) {
        super(HttpStatus.FORBIDDEN, mensagem);
    }

    public AcessoNegadoException() {
        this(AUTOR_EDITAR_TOPICO);
    }

    public static AcessoNegadoException autorEditarTopico() {
        return new AcessoNegadoException(AUTOR_EDITAR_TOPICO);
    }

    public static AcessoNegadoException autorOuAdministradorAlterarTopico() {
        return new AcessoNegadoException(AUTOR_OU_ADMIN_ALTERAR_TOPICO);
    }

    public static AcessoNegadoException administradorApagarTopico() {
        return new AcessoNegadoException(ADMIN_APAGAR_TOPICO);
    }

    public static AcessoNegadoException autorEditarComentario() {
        return new AcessoNegadoException(AUTOR_EDITAR_COMENTARIO);
    }

    public static AcessoNegadoException autorOuAdministradorApagarComentario() {
        return new AcessoNegadoException(AUTOR_OU_ADMIN_APAGAR_COMENTARIO);
    }
}
