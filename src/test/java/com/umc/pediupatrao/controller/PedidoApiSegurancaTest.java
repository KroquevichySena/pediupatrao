package com.umc.pediupatrao.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prova que as restrições valem na API, independente da tela:
 * só o GERENTE consegue cancelar e dar desconto, inclusive em comparação ao ADMIN.
 */
@WebMvcTest(PedidoController.class)
@Import(SecurityConfig.class)
class PedidoApiSegurancaTest {

    private static final String CANCELAMENTO = "{\"justificativa\":\"cliente desistiu do pedido\"}";
    private static final String DESCONTO = "{\"percentual\":10}";

    @Autowired MockMvc mvc;
    @MockitoBean PedidoService pedidoService;
    @MockitoBean UsuarioDetailsService usuarioDetailsService;

    @Test
    void semLoginRecebe401() throws Exception {
        mvc.perform(get("/api/pedidos")).andExpect(status().isUnauthorized());
    }

    @Test
    void atendenteNaoCancela() throws Exception {
        mvc.perform(post("/api/pedidos/1/cancelamento").with(user("atendente01").roles("ATENDENTE")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(CANCELAMENTO))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminNaoCancelaNemDaDesconto() throws Exception {
        mvc.perform(post("/api/pedidos/1/cancelamento").with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(CANCELAMENTO))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/pedidos/1/desconto").with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(DESCONTO))
                .andExpect(status().isForbidden());
    }

    @Test
    void gerenteCancela() throws Exception {
        when(pedidoService.cancelar(anyString(), anyString())).thenReturn(new Pedido());
        mvc.perform(post("/api/pedidos/1/cancelamento").with(user("gerente01").roles("GERENTE")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(CANCELAMENTO))
                .andExpect(status().isOk());
    }

    @Test
    void gerenteDaDesconto() throws Exception {
        when(pedidoService.aplicarDesconto(anyString(), any())).thenReturn(new Pedido());
        mvc.perform(post("/api/pedidos/1/desconto").with(user("gerente01").roles("GERENTE")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(DESCONTO))
                .andExpect(status().isOk());
    }

    @Test
    void semTokenCsrfRecebe403() throws Exception {
        mvc.perform(post("/api/pedidos/1/cancelamento").with(user("gerente01").roles("GERENTE"))
                .contentType(MediaType.APPLICATION_JSON).content(CANCELAMENTO))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminNaoRegistraPedido() throws Exception {
        mvc.perform(post("/api/pedidos").with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }
}
