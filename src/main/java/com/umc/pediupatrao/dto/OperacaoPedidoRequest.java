package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.StatusPedido;
import java.math.BigDecimal;


public final class OperacaoPedidoRequest {

    private OperacaoPedidoRequest() {
    }

    public record MudancaStatus(StatusPedido status) {
    }

    public record Desconto(BigDecimal percentual) {
    }

    public record Cancelamento(String justificativa) {
    }
}
