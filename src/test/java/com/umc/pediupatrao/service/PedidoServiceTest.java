package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.dto.NovoPedidoRequest;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoEntrega;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.security.UsuarioLogado;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PedidoServiceTest {

    @Mock PedidoRepository pedidoRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock ProdutoRepository produtoRepository;
    @Mock AuditoriaService auditoriaService;
    @Mock UsuarioLogado usuarioLogado;

    @InjectMocks PedidoService service;

    @BeforeEach
    void setUp() {
        when(usuarioLogado.username()).thenReturn("usuario.teste");
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId("abc123456");
            }
            return p;
        });
    }

    // ---------- RF02: valores calculados no servidor ----------

    @Test
    void criarPedidoCalculaTotalComPrecoDoCadastro() {
        Cliente cliente = new Cliente();
        cliente.setId("c1");
        cliente.setNome("Maria");
        when(clienteRepository.findById("c1")).thenReturn(Optional.of(cliente));
        when(produtoRepository.findById("p1")).thenReturn(Optional.of(produto("p1", "Calabresa", 50.0, true)));
        when(produtoRepository.findById("p2")).thenReturn(Optional.of(produto("p2", "Refri", 12.5, true)));

        Pedido pedido = service.criarPedido(new NovoPedidoRequest("c1", TipoEntrega.ENTREGA, null,
                List.of(new NovoPedidoRequest.Item("p1", 2), new NovoPedidoRequest.Item("p2", 1))));

        assertEquals(new BigDecimal("112.50"), pedido.getValorTotal());
        assertEquals(StatusPedido.RECEBIDO, pedido.getStatus());
        assertEquals("usuario.teste", pedido.getRegistradoPor());
        assertNotNull(pedido.getDataEntrada());
        verify(auditoriaService).registrar(eq(TipoOperacao.INCLUSAO_PEDIDO), eq("PEDIDO"), eq("abc123456"), anyList(), isNull());
    }

    @Test
    void naoAceitaProdutoInativo() {
        Cliente cliente = new Cliente();
        cliente.setId("c1");
        when(clienteRepository.findById("c1")).thenReturn(Optional.of(cliente));
        when(produtoRepository.findById("p1")).thenReturn(Optional.of(produto("p1", "Antiga", 30.0, false)));

        assertThrows(RegraNegocioException.class, () -> service.criarPedido(new NovoPedidoRequest("c1",
                TipoEntrega.RETIRADA, null, List.of(new NovoPedidoRequest.Item("p1", 1)))));
    }

    @Test
    void naoAceitaQuantidadeZero() {
        Cliente cliente = new Cliente();
        cliente.setId("c1");
        when(clienteRepository.findById("c1")).thenReturn(Optional.of(cliente));

        assertThrows(RegraNegocioException.class, () -> service.criarPedido(new NovoPedidoRequest("c1",
                TipoEntrega.RETIRADA, null, List.of(new NovoPedidoRequest.Item("p1", 0)))));
    }

    // ---------- RF03: transições ----------

    @Test
    void naoPermiteFinalizarPedidoEmPreparacao() {
        mockPedido(StatusPedido.EM_PREPARACAO, TipoEntrega.ENTREGA);
        assertThrows(RegraNegocioException.class, () -> service.atualizarStatus("x", StatusPedido.FINALIZADO));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void saidaRegistraResponsavelEDataHora() {
        mockPedido(StatusPedido.PRONTO, TipoEntrega.ENTREGA);
        Pedido pedido = service.atualizarStatus("x", StatusPedido.SAIU_PARA_ENTREGA);
        assertEquals("usuario.teste", pedido.getSaidaRegistradaPor());
        assertNotNull(pedido.getDataSaida());
    }

    @Test
    void pedidoDeRetiradaNaoSaiParaEntrega() {
        mockPedido(StatusPedido.PRONTO, TipoEntrega.RETIRADA);
        assertThrows(RegraNegocioException.class, () -> service.atualizarStatus("x", StatusPedido.SAIU_PARA_ENTREGA));
    }

    @Test
    void pedidoCanceladoNaoMudaDeStatus() {
        mockPedido(StatusPedido.CANCELADO, TipoEntrega.ENTREGA);
        assertThrows(RegraNegocioException.class, () -> service.atualizarStatus("x", StatusPedido.EM_PREPARACAO));
    }

    // ---------- RF04: desconto e cancelamento ----------

    @Test
    void descontoAcimaDe20PorCentoEhRecusado() {
        mockPedido(StatusPedido.RECEBIDO, TipoEntrega.ENTREGA);
        assertThrows(RegraNegocioException.class, () -> service.aplicarDesconto("x", new BigDecimal("20.01")));
    }

    @Test
    void descontoRecalculaTotal() {
        mockPedido(StatusPedido.EM_PREPARACAO, TipoEntrega.ENTREGA);
        Pedido pedido = service.aplicarDesconto("x", new BigDecimal("10"));
        assertEquals(new BigDecimal("10.00"), pedido.getValorDesconto());
        assertEquals(new BigDecimal("90.00"), pedido.getValorTotal());
        verify(auditoriaService).registrar(eq(TipoOperacao.DESCONTO_PEDIDO), eq("PEDIDO"), eq("x"), anyList(), isNull());
    }

    @Test
    void descontoDepoisDaSaidaEhRecusado() {
        mockPedido(StatusPedido.SAIU_PARA_ENTREGA, TipoEntrega.ENTREGA);
        assertThrows(RegraNegocioException.class, () -> service.aplicarDesconto("x", new BigDecimal("5")));
    }

    @Test
    void cancelamentoExigeJustificativa() {
        mockPedido(StatusPedido.RECEBIDO, TipoEntrega.ENTREGA);
        assertThrows(RegraNegocioException.class, () -> service.cancelar("x", "curta"));
    }

    @Test
    void cancelamentoDepoisDaSaidaEhRecusado() {
        mockPedido(StatusPedido.RETIRADO, TipoEntrega.RETIRADA);
        assertThrows(RegraNegocioException.class, () -> service.cancelar("x", "cliente desistiu do pedido"));
    }

    @Test
    void cancelamentoPreservaValoresERegistraJustificativa() {
        mockPedido(StatusPedido.PRONTO, TipoEntrega.ENTREGA);
        Pedido pedido = service.cancelar("x", "cliente desistiu do pedido");
        assertEquals(StatusPedido.CANCELADO, pedido.getStatus());
        assertEquals(StatusPedido.PRONTO, pedido.getStatusAntesCancelamento());
        assertEquals(new BigDecimal("100.00"), pedido.getValorTotal());
        verify(auditoriaService).registrar(eq(TipoOperacao.CANCELAMENTO_PEDIDO), eq("PEDIDO"), eq("x"), anyList(),
                eq("cliente desistiu do pedido"));
    }

    // ---------- utilitários ----------

    private void mockPedido(StatusPedido status, TipoEntrega tipo) {
        Pedido p = new Pedido();
        p.setId("x");
        p.setStatus(status);
        p.setTipoEntrega(tipo);
        p.getItens().add(new ItemPedido("p1", "Pizza", 2, new BigDecimal("50.00")));
        PedidoService.recalcularValores(p);
        when(pedidoRepository.findById("x")).thenReturn(Optional.of(p));
    }

    private static Produto produto(String id, String nome, double preco, boolean ativo) {
        Produto p = new Produto();
        p.setId(id);
        p.setNome(nome);
        p.setPreco(preco);
        p.setAtivo(ativo);
        return p;
    }
}
