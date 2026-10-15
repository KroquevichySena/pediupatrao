package com.umc.pediupatrao.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "pedidos")
public class Pedido {

    @Id
    private String id;
    private String clienteId;
    private String clienteNome;
    private TipoEntrega tipoEntrega;
    private String observacao;
    private StatusPedido status;

    // Itens e valores (sempre calculados no servidor)
    private List<ItemPedido> itens = new ArrayList<>();
    private BigDecimal valorItens = BigDecimal.ZERO;
    private BigDecimal percentualDesconto = BigDecimal.ZERO;
    private BigDecimal valorDesconto = BigDecimal.ZERO;
    private BigDecimal valorTotal = BigDecimal.ZERO;
    private String descontoAplicadoPor;

    // Entrada
    private String registradoPor;
    private LocalDateTime dataEntrada;

    // Saída
    private String saidaRegistradaPor;
    private LocalDateTime dataSaida;

    // Cancelamento
    private String canceladoPor;
    private LocalDateTime dataCancelamento;
    private String justificativaCancelamento;
    private StatusPedido statusAntesCancelamento;

    private List<MovimentacaoPedido> historico = new ArrayList<>();

    // Campos do modelo antigo, mantidos só para leitura de pedidos já gravados
    private String tipoPizza;
    private String sabor;
    private Integer quantidade;

    public boolean isCancelado() {
        return status == StatusPedido.CANCELADO;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }

    public String getClienteNome() { return clienteNome; }
    public void setClienteNome(String clienteNome) { this.clienteNome = clienteNome; }

    public TipoEntrega getTipoEntrega() { return tipoEntrega; }
    public void setTipoEntrega(TipoEntrega tipoEntrega) { this.tipoEntrega = tipoEntrega; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    public StatusPedido getStatus() { return status; }
    public void setStatus(StatusPedido status) { this.status = status; }

    public List<ItemPedido> getItens() { return itens; }
    public void setItens(List<ItemPedido> itens) { this.itens = itens; }

    public BigDecimal getValorItens() { return valorItens; }
    public void setValorItens(BigDecimal valorItens) { this.valorItens = valorItens; }

    public BigDecimal getPercentualDesconto() { return percentualDesconto; }
    public void setPercentualDesconto(BigDecimal percentualDesconto) { this.percentualDesconto = percentualDesconto; }

    public BigDecimal getValorDesconto() { return valorDesconto; }
    public void setValorDesconto(BigDecimal valorDesconto) { this.valorDesconto = valorDesconto; }

    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }

    public String getDescontoAplicadoPor() { return descontoAplicadoPor; }
    public void setDescontoAplicadoPor(String descontoAplicadoPor) { this.descontoAplicadoPor = descontoAplicadoPor; }

    public String getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(String registradoPor) { this.registradoPor = registradoPor; }

    public LocalDateTime getDataEntrada() { return dataEntrada; }
    public void setDataEntrada(LocalDateTime dataEntrada) { this.dataEntrada = dataEntrada; }

    public String getSaidaRegistradaPor() { return saidaRegistradaPor; }
    public void setSaidaRegistradaPor(String saidaRegistradaPor) { this.saidaRegistradaPor = saidaRegistradaPor; }

    public LocalDateTime getDataSaida() { return dataSaida; }
    public void setDataSaida(LocalDateTime dataSaida) { this.dataSaida = dataSaida; }

    public String getCanceladoPor() { return canceladoPor; }
    public void setCanceladoPor(String canceladoPor) { this.canceladoPor = canceladoPor; }

    public LocalDateTime getDataCancelamento() { return dataCancelamento; }
    public void setDataCancelamento(LocalDateTime dataCancelamento) { this.dataCancelamento = dataCancelamento; }

    public String getJustificativaCancelamento() { return justificativaCancelamento; }
    public void setJustificativaCancelamento(String justificativaCancelamento) { this.justificativaCancelamento = justificativaCancelamento; }

    public StatusPedido getStatusAntesCancelamento() { return statusAntesCancelamento; }
    public void setStatusAntesCancelamento(StatusPedido statusAntesCancelamento) { this.statusAntesCancelamento = statusAntesCancelamento; }

    public List<MovimentacaoPedido> getHistorico() { return historico; }
    public void setHistorico(List<MovimentacaoPedido> historico) { this.historico = historico; }

    public String getTipoPizza() { return tipoPizza; }
    public void setTipoPizza(String tipoPizza) { this.tipoPizza = tipoPizza; }

    public String getSabor() { return sabor; }
    public void setSabor(String sabor) { this.sabor = sabor; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
}
