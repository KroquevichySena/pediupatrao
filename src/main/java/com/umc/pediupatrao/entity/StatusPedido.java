package com.umc.pediupatrao.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Fluxo do pedido:
 * RECEBIDO -> EM_PREPARACAO -> PRONTO -> SAIU_PARA_ENTREGA | RETIRADO -> FINALIZADO
 * CANCELADO é um estado excepcional, tratado por uma operação própria (só GERENTE).
 */
public enum StatusPedido {
    RECEBIDO("Recebido"),
    EM_PREPARACAO("Em preparação"),
    PRONTO("Pronto"),
    SAIU_PARA_ENTREGA("Saiu para entrega"),
    RETIRADO("Retirado"),
    FINALIZADO("Finalizado"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusPedido(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Próximos status permitidos no fluxo normal (o cancelamento é outra operação). */
    public Set<StatusPedido> proximosPermitidos() {
        return switch (this) {
            case RECEBIDO -> EnumSet.of(EM_PREPARACAO);
            case EM_PREPARACAO -> EnumSet.of(PRONTO);
            case PRONTO -> EnumSet.of(SAIU_PARA_ENTREGA, RETIRADO);
            case SAIU_PARA_ENTREGA, RETIRADO -> EnumSet.of(FINALIZADO);
            case FINALIZADO, CANCELADO -> EnumSet.noneOf(StatusPedido.class);
        };
    }

    public boolean podeIrPara(StatusPedido destino) {
        return proximosPermitidos().contains(destino);
    }

    /** Status que representam a saída do pedido do estabelecimento. */
    public boolean isSaida() {
        return this == SAIU_PARA_ENTREGA || this == RETIRADO;
    }

    /** Enquanto o pedido não saiu, ainda é possível cancelar ou dar desconto. */
    public boolean isAntesDaSaida() {
        return this == RECEBIDO || this == EM_PREPARACAO || this == PRONTO;
    }
}
