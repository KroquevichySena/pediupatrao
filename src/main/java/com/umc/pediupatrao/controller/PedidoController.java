package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.NovoPedidoRequest;
import com.umc.pediupatrao.dto.OperacaoPedidoRequest;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.service.PedidoService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','ATENDENTE')")
    public List<Pedido> listarPedidos() {
        return pedidoService.listarPedidos();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','ATENDENTE')")
    public Pedido buscar(@PathVariable String id) {
        return pedidoService.buscarPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ATENDENTE')")
    public ResponseEntity<Pedido> criarPedido(@RequestBody NovoPedidoRequest pedido) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.criarPedido(pedido));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ATENDENTE','GERENTE')")
    public Pedido atualizarStatus(@PathVariable String id, @RequestBody OperacaoPedidoRequest.MudancaStatus corpo) {
        return pedidoService.atualizarStatus(id, corpo.status());
    }

    // Rota original do projeto, mantida por compatibilidade (status pela query string)
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ATENDENTE','GERENTE')")
    public Pedido atualizarStatusLegado(@PathVariable String id, @RequestParam String status) {
        return pedidoService.atualizarStatus(id, StatusPedido.valueOf(status.trim().toUpperCase()));
    }

    @PostMapping("/{id}/desconto")
    @PreAuthorize("hasRole('GERENTE')")
    public Pedido aplicarDesconto(@PathVariable String id, @RequestBody OperacaoPedidoRequest.Desconto corpo) {
        return pedidoService.aplicarDesconto(id, corpo.percentual());
    }

    @PostMapping("/{id}/cancelamento")
    @PreAuthorize("hasRole('GERENTE')")
    public Pedido cancelar(@PathVariable String id, @RequestBody OperacaoPedidoRequest.Cancelamento corpo) {
        return pedidoService.cancelar(id, corpo.justificativa());
    }
}
