package com.umc.pediupatrao.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.MovimentacaoPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.RegistroAuditoria;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoEntrega;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.security.UsuarioLogado;
import com.umc.pediupatrao.service.AuditoriaService;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.service.ProdutoService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import com.umc.pediupatrao.service.UsuarioService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Garante que as telas Thymeleaf renderizam para cada perfil e que o menu respeita o perfil. */
@WebMvcTest({HomeController.class, PedidoWebController.class, AuditoriaWebController.class})
@Import(SecurityConfig.class)
class TelasRenderizacaoTest {

    @Autowired MockMvc mvc;
    @MockitoBean PedidoService pedidoService;
    @MockitoBean ClienteService clienteService;
    @MockitoBean ProdutoService produtoService;
    @MockitoBean UsuarioService usuarioService;
    @MockitoBean AuditoriaService auditoriaService;
    @MockitoBean UsuarioDetailsService usuarioDetailsService;
    @MockitoBean UsuarioLogado usuarioLogado;

    @BeforeEach
    void dados() {
        Pedido pedido = new Pedido();
        pedido.setId("664f1a2b3c4d5e6f7a8b9c0d");
        pedido.setClienteNome("Maria");
        pedido.setTipoEntrega(TipoEntrega.ENTREGA);
        pedido.setStatus(StatusPedido.PRONTO);
        pedido.getItens().add(new ItemPedido("p1", "Pizza Calabresa", 2, new BigDecimal("54.90")));
        pedido.setValorItens(new BigDecimal("109.80"));
        pedido.setPercentualDesconto(new BigDecimal("10"));
        pedido.setValorDesconto(new BigDecimal("10.98"));
        pedido.setValorTotal(new BigDecimal("98.82"));
        pedido.setRegistradoPor("atendente01");
        pedido.setDataEntrada(LocalDateTime.now());
        pedido.getHistorico().add(new MovimentacaoPedido(StatusPedido.RECEBIDO, "atendente01", LocalDateTime.now()));

        RegistroAuditoria registro = new RegistroAuditoria("gerente01", "GERENTE", LocalDateTime.now(),
                TipoOperacao.DESCONTO_PEDIDO, "PEDIDO", pedido.getId(),
                List.of(new AlteracaoCampo("percentualDesconto", "0", "10")), null);

        Cliente cliente = new Cliente();
        cliente.setId("c1");
        cliente.setNome("Maria");
        cliente.setTelefone("(11) 99999-1234");
        Produto produto = new Produto();
        produto.setId("p1");
        produto.setNome("Pizza Calabresa");
        produto.setPreco(54.90);
        produto.setAtivo(true);
        Usuario usuario = new Usuario();
        usuario.setId("u1");
        usuario.setUsername("gerente01");
        usuario.setRole("GERENTE");

        when(pedidoService.listarPedidos()).thenReturn(List.of(pedido));
        when(pedidoService.buscarPorId(anyString())).thenReturn(pedido);
        when(auditoriaService.consultar(any(), any())).thenReturn(List.of(registro));
        when(clienteService.listarClientes()).thenReturn(List.of(cliente));
        when(clienteService.buscarPorId("c1")).thenReturn(Optional.of(cliente));
        when(produtoService.listarProdutos()).thenReturn(List.of(produto));
        when(produtoService.listarAtivos()).thenReturn(List.of(produto));
        when(usuarioService.listarTodos()).thenReturn(List.of(usuario));
        when(usuarioService.buscarPorId("u1")).thenReturn(Optional.of(usuario));
    }

    @Test
    void telasDoGerente() throws Exception {
        var gerente = user("gerente01").roles("GERENTE");
        mvc.perform(get("/pedidos").with(gerente)).andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Pronto")));
        mvc.perform(get("/pedidos/664f1a2b3c4d5e6f7a8b9c0d").with(gerente)).andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Cancelar pedido")))
                .andExpect(content().string(Matchers.containsString("Auditoria deste pedido")));
        mvc.perform(get("/auditoria").with(gerente)).andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("percentualDesconto")));
        mvc.perform(get("/clientes").with(gerente)).andExpect(status().isOk());
        mvc.perform(get("/produtos").with(gerente)).andExpect(status().isOk());
        mvc.perform(get("/").with(gerente)).andExpect(status().isOk())
                .andExpect(content().string(Matchers.not(Matchers.containsString("/usuarios"))));
    }

    @Test
    void telasDoAtendente() throws Exception {
        var atendente = user("atendente01").roles("ATENDENTE");
        mvc.perform(get("/pedidos/novo").with(atendente)).andExpect(status().isOk());
        mvc.perform(get("/pedidos/664f1a2b3c4d5e6f7a8b9c0d").with(atendente)).andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Saiu para entrega")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("Cancelar pedido"))));
        mvc.perform(get("/clientes/novo").with(atendente)).andExpect(status().isOk());
        mvc.perform(get("/clientes/editar/c1").with(atendente)).andExpect(status().isOk());
        mvc.perform(get("/auditoria").with(atendente)).andExpect(status().isForbidden());
    }

    @Test
    void telasDoAdmin() throws Exception {
        var admin = user("admin").roles("ADMIN");
        mvc.perform(get("/usuarios").with(admin)).andExpect(status().isOk());
        mvc.perform(get("/usuarios/novo").with(admin)).andExpect(status().isOk());
        mvc.perform(get("/usuarios/editar/u1").with(admin)).andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("ATENDENTE")));
        mvc.perform(get("/pedidos/novo").with(admin)).andExpect(status().isForbidden());
        mvc.perform(get("/clientes").with(admin)).andExpect(status().isForbidden());
    }

    @Test
    void telaDeLogin() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
    }
}
