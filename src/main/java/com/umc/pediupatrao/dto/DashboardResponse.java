package com.umc.pediupatrao.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DashboardResponse(LocalDate inicio, LocalDate fim, LocalDateTime atualizadoEm,
        Indicadores indicadores, List<Venda> vendas, List<Situacao> situacoes,
        List<ProdutoVendido> produtos, Pagina pedidos) {
    public record Indicadores(long totalPedidos, long finalizados, long emAndamento, long cancelados,
            BigDecimal faturamento, BigDecimal ticketMedio, BigDecimal descontos, BigDecimal taxaCancelamento) {}
    public record Venda(String periodo, BigDecimal faturamento, long pedidos) {}
    public record Situacao(String status, String descricao, long quantidade) {}
    public record ProdutoVendido(String produtoId, String nome, long quantidade, BigDecimal valorBruto) {}
    public record PedidoResumo(String id, LocalDateTime dataEntrada, String status, String tipoEntrega,
            BigDecimal valorTotal) {}
    public record Pagina(List<PedidoResumo> itens, int pagina, int tamanho, long total, int totalPaginas) {}
}
