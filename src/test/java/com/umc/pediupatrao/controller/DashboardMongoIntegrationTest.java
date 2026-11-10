package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.*;
import com.umc.pediupatrao.repository.PedidoRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Integração real: HTTP -> Security -> Controller -> Service -> MongoDB -> JSON. */
@SpringBootTest(properties={"spring.data.mongodb.database=pediupatrao_dashboard_test"})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named="RUN_MONGO_TESTS", matches="true")
@WithMockUser(roles="GERENTE")
class DashboardMongoIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired PedidoRepository repo;
    final List<String> ids=List.of("it-dashboard-a","it-dashboard-b","it-dashboard-c","it-dashboard-d");
    @BeforeEach void preparar() {
        repo.deleteAllById(ids);
        salvar(ids.get(0),"2024-02-01T00:00:00",StatusPedido.FINALIZADO,TipoEntrega.ENTREGA,"90.00");
        salvar(ids.get(1),"2024-02-29T23:59:59",StatusPedido.FINALIZADO,TipoEntrega.RETIRADA,"90.00");
        salvar(ids.get(2),"2024-02-15T18:00:00",StatusPedido.CANCELADO,TipoEntrega.ENTREGA,"90.00");
        salvar(ids.get(3),"2024-03-01T00:00:00",StatusPedido.FINALIZADO,TipoEntrega.ENTREGA,"90.00");
    }
    @AfterEach void limpar() { repo.deleteAllById(ids); }
    void salvar(String id,String data,StatusPedido status,TipoEntrega tipo,String valor) {
        Pedido p=new Pedido();p.setId(id);p.setDataEntrada(LocalDateTime.parse(data));p.setStatus(status);p.setTipoEntrega(tipo);
        p.setValorTotal(new BigDecimal(valor));p.setValorDesconto(new BigDecimal("10.00"));
        p.setItens(List.of(new ItemPedido("pizza-it","Pizza Teste",2,new BigDecimal("50"))));repo.save(p);
    }
    @Test void mongoRespeitaLimitesDoPeriodoESomaValores() throws Exception {
        mvc.perform(get("/api/dashboard?inicio=2024-02-01&fim=2024-02-29&agrupamento=mes"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.indicadores.totalPedidos").value(3))
                .andExpect(jsonPath("$.indicadores.faturamento").value(180.00))
                .andExpect(jsonPath("$.indicadores.ticketMedio").value(90.00))
                .andExpect(jsonPath("$.indicadores.cancelados").value(1))
                .andExpect(jsonPath("$.vendas.length()").value(1))
                .andExpect(jsonPath("$.produtos[0].quantidade").value(4))
                .andExpect(jsonPath("$.pedidos.itens[0].id").value(ids.get(1)));
    }
    @Test void mongoAplicaFiltrosCombinados() throws Exception {
        mvc.perform(get("/api/dashboard?inicio=2024-02-01&fim=2024-02-29&status=FINALIZADO&tipoEntrega=RETIRADA"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.indicadores.totalPedidos").value(1))
                .andExpect(jsonPath("$.indicadores.faturamento").value(90.00))
                .andExpect(jsonPath("$.pedidos.itens[0].tipoEntrega").value("RETIRADA"));
    }
}
