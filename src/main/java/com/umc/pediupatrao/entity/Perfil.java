package com.umc.pediupatrao.entity;

/**
 * Perfis de acesso do sistema. O nome do enum vira a authority "ROLE_<nome>" no Spring Security.
 */
public enum Perfil {
    ADMIN,
    GERENTE,
    ATENDENTE;

    public static Perfil deTexto(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Perfil não informado.");
        }
        try {
            return Perfil.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Perfil inválido: " + valor);
        }
    }
}
