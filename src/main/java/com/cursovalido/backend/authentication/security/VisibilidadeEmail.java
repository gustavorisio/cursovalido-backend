package com.cursovalido.backend.authentication.security;

public final class VisibilidadeEmail {
    private VisibilidadeEmail() {
    }

    public static String paraListagem(String email, String perfil, boolean administrador) {
        if (administrador && ("PROFESSOR".equals(perfil) || "ADMINISTRADOR".equals(perfil))) {
            return email;
        }
        return mascarar(email);
    }

    public static String mascarar(String email) {
        if (email == null) return "*******";
        int arroba = email.lastIndexOf('@');
        if (arroba <= 0 || arroba == email.length() - 1) return "*******";
        return email.substring(0, email.offsetByCodePoints(0, 1)) + "*******" + email.substring(arroba);
    }
}
