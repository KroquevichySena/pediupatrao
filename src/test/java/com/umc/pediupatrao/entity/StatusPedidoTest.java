package com.umc.pediupatrao.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StatusPedidoTest {

    @Test
    void fluxoNormalEhPermitido() {
        assertTrue(StatusPedido.RECEBIDO.podeIrPara(StatusPedido.EM_PREPARACAO));
        assertTrue(StatusPedido.EM_PREPARACAO.podeIrPara(StatusPedido.PRONTO));
        assertTrue(StatusPedido.PRONTO.podeIrPara(StatusPedido.SAIU_PARA_ENTREGA));
        assertTrue(StatusPedido.PRONTO.podeIrPara(StatusPedido.RETIRADO));
        assertTrue(StatusPedido.SAIU_PARA_ENTREGA.podeIrPara(StatusPedido.FINALIZADO));
        assertTrue(StatusPedido.RETIRADO.podeIrPara(StatusPedido.FINALIZADO));
    }

    @Test
    void naoPermitePularEtapas() {
        assertFalse(StatusPedido.EM_PREPARACAO.podeIrPara(StatusPedido.FINALIZADO));
        assertFalse(StatusPedido.RECEBIDO.podeIrPara(StatusPedido.PRONTO));
        assertFalse(StatusPedido.PRONTO.podeIrPara(StatusPedido.RECEBIDO));
    }

    @Test
    void estadosFinaisNaoTemProximos() {
        assertTrue(StatusPedido.CANCELADO.proximosPermitidos().isEmpty());
        assertTrue(StatusPedido.FINALIZADO.proximosPermitidos().isEmpty());
    }

    @Test
    void cancelamentoNaoFazParteDoFluxoNormal() {
        for (StatusPedido s : StatusPedido.values()) {
            assertFalse(s.podeIrPara(StatusPedido.CANCELADO));
        }
    }
}
