package com.umc.pediupatrao.entity;

import java.time.LocalDateTime;

/** Linha do histórico de status de um pedido (quem mudou, quando e para qual status). */
public class MovimentacaoPedido {

    private StatusPedido status;
    private String usuario;
    private LocalDateTime dataHora;

    public MovimentacaoPedido() {
    }

    public MovimentacaoPedido(StatusPedido status, String usuario, LocalDateTime dataHora) {
        this.status = status;
        this.usuario = usuario;
        this.dataHora = dataHora;
    }

    public StatusPedido getStatus() { return status; }
    public void setStatus(StatusPedido status) { this.status = status; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
}
