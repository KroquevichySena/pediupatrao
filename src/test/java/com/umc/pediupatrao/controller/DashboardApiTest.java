package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({DashboardController.class,DashboardWebController.class})
@Import(SecurityConfig.class)
class DashboardApiTest {
    @Autowired MockMvc mvc;
    @MockitoBean DashboardService service;
    @MockitoBean UsuarioDetailsService usuarios;
    @Test void exigeLogin() throws Exception { mvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized()); }
    @Test void bloqueiaAtendente() throws Exception {
        mvc.perform(get("/api/dashboard").with(user("a").roles("ATENDENTE"))).andExpect(status().isForbidden());
        mvc.perform(get("/dashboard").with(user("a").roles("ATENDENTE"))).andExpect(status().isForbidden());
    }
    @Test void adminEGerenteConsultamFiltros() throws Exception {
        for(String perfil:new String[]{"ADMIN","GERENTE"}) mvc.perform(get("/api/dashboard/filtros").with(user("u").roles(perfil)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status.length()").value(7)).andExpect(jsonPath("$.tiposEntrega[0]").value("ENTREGA"));
    }
    @Test void renderizaDashboard() throws Exception {
        mvc.perform(get("/dashboard").with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Visão da operação")));
    }
    @Test void parametroMalformadoRetorna400() throws Exception {
        for(String query:new String[]{"inicio=ontem","status=INVALIDO","pagina=abc","tipoEntrega=DRONE"})
            mvc.perform(get("/api/dashboard?"+query).with(user("u").roles("GERENTE")))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }
    @Test void regraRetorna422() throws Exception {
        when(service.consultar(any(),any(),any(),any(),anyString(),anyInt(),anyInt())).thenThrow(new RegraNegocioException("Período inválido"));
        mvc.perform(get("/api/dashboard").with(user("u").roles("ADMIN"))).andExpect(status().isUnprocessableEntity());
    }
    @Test void falhaDeBancoRetorna503SemDetalhesInternos() throws Exception {
        when(service.consultar(any(),any(),any(),any(),anyString(),anyInt(),anyInt())).thenThrow(new DataAccessResourceFailureException("segredo-interno"));
        mvc.perform(get("/api/dashboard").with(user("u").roles("ADMIN"))).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensagem").value("Banco de dados indisponível. Tente novamente."));
    }
    @Test void escritaNaoPermitida() throws Exception {
        mvc.perform(post("/api/dashboard").with(user("u").roles("ADMIN")).with(csrf())).andExpect(status().isForbidden());
    }
}
