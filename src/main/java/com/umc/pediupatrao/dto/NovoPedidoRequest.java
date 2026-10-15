package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.TipoEntrega;
import java.util.List;


public record NovoPedidoRequest(
        String clienteId,
        TipoEntrega tipoEntrega,
        String observacao,
        List<Item> itens) {

    public record Item(String produtoId, Integer quantidade) {
    }
}
