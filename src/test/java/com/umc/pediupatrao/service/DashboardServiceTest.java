package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.*;
import com.umc.pediupatrao.exception.RegraNegocioException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DashboardServiceTest {
    MongoTemplate mongo;
    DashboardService service;
    LocalDate inicio = LocalDate.of(2026, 11, 1), fim = LocalDate.of(2026, 11, 3);
    @BeforeEach void preparar() { mongo = mock(MongoTemplate.class); service = new DashboardService(mongo); }
    Pedido pedido(String id, StatusPedido status, String valor, int dia) {
        Pedido p = new Pedido(); p.setId(id); p.setStatus(status); p.setValorTotal(new BigDecimal(valor));
        p.setDataEntrada(inicio.withDayOfMonth(dia).atTime(23, 59)); p.setTipoEntrega(TipoEntrega.ENTREGA);
        return p;
    }
    @Test void calculaReceitaLiquidaTicketRankingEStatus() {
        Pedido a = pedido("a",StatusPedido.FINALIZADO,"90",1);
        a.setValorDesconto(new BigDecimal("10")); a.setItens(List.of(new ItemPedido("p1","Pizza",2,new BigDecimal("50"))));
        Pedido b = pedido("b",StatusPedido.FINALIZADO,"50",1);
        b.setItens(List.of(new ItemPedido("p1","Pizza",1,new BigDecimal("50"))));
        when(mongo.find(any(Query.class),eq(Pedido.class))).thenReturn(List.of(a,b,pedido("c",StatusPedido.CANCELADO,"999",2),pedido("d",StatusPedido.RECEBIDO,"300",3)));
        var d=service.consultar(inicio,fim,null,null,"dia",5,0);
        assertEquals(new BigDecimal("140.00"),d.indicadores().faturamento());
        assertEquals(new BigDecimal("70.00"),d.indicadores().ticketMedio());
        assertEquals(new BigDecimal("10.00"),d.indicadores().descontos());
        assertEquals(new BigDecimal("25.00"),d.indicadores().taxaCancelamento());
        assertEquals(1,d.indicadores().emAndamento()); assertEquals(4,d.indicadores().totalPedidos());
        assertEquals(3,d.produtos().getFirst().quantidade()); assertEquals(new BigDecimal("150"),d.produtos().getFirst().valorBruto());
        assertEquals(3,d.vendas().size()); assertEquals(new BigDecimal("0.00"),d.vendas().get(1).faturamento());
        assertEquals(4,d.situacoes().stream().mapToLong(s->s.quantidade()).sum());
    }
    @Test void vazioNaoDividePorZero() {
        when(mongo.find(any(Query.class),eq(Pedido.class))).thenReturn(List.of());
        var d=service.consultar(inicio,fim,null,null,"mes",5,0);
        assertEquals(new BigDecimal("0.00"),d.indicadores().ticketMedio());
        assertEquals(1,d.vendas().size()); assertTrue(d.produtos().isEmpty()); assertEquals(0,d.pedidos().totalPaginas());
    }
    @Test void consultaBancoComLimiteExclusivoEFiltros() {
        when(mongo.find(any(Query.class),eq(Pedido.class))).thenReturn(List.of());
        service.consultar(inicio,fim,StatusPedido.FINALIZADO,TipoEntrega.RETIRADA,"dia",5,0);
        var captor=ArgumentCaptor.forClass(Query.class); verify(mongo).find(captor.capture(),eq(Pedido.class));
        var q=captor.getValue().getQueryObject();
        assertEquals(StatusPedido.FINALIZADO,q.get("status")); assertEquals(TipoEntrega.RETIRADA,q.get("tipoEntrega"));
        var datas=(org.bson.Document)q.get("dataEntrada");
        assertEquals(inicio.atStartOfDay(),datas.get("$gte")); assertEquals(fim.plusDays(1).atStartOfDay(),datas.get("$lt"));
    }
    @Test void rejeitaParametrosInvalidosAntesDeAcessarBanco() {
        assertThrows(RegraNegocioException.class,()->service.consultar(fim,inicio,null,null,"dia",5,0));
        assertThrows(RegraNegocioException.class,()->service.consultar(inicio,inicio.plusDays(366),null,null,"dia",5,0));
        assertThrows(RegraNegocioException.class,()->service.consultar(inicio,fim,null,null,"semana",5,0));
        assertThrows(RegraNegocioException.class,()->service.consultar(inicio,fim,null,null,"dia",21,0));
        assertThrows(RegraNegocioException.class,()->service.consultar(inicio,fim,null,null,"dia",5,-1));
        verifyNoInteractions(mongo);
    }
    @Test void legadoComNulosNaoDerrubaPainel() {
        Pedido p=pedido("antigo",null,"0",1);p.setValorTotal(null);p.setItens(null);
        when(mongo.find(any(Query.class),eq(Pedido.class))).thenReturn(List.of(p));
        var d=service.consultar(inicio,fim,null,null,"dia",5,0);
        assertEquals(1,d.situacoes().stream().filter(s->s.status().equals("SEM_STATUS")).findFirst().orElseThrow().quantidade());
        assertEquals("SEM_STATUS",d.pedidos().itens().getFirst().status());
    }
    @Test void paginaSemDuplicarPedidos() {
        var pedidos=java.util.stream.IntStream.range(0,23).mapToObj(i->pedido("p"+i,StatusPedido.FINALIZADO,"10",1)).toList();
        when(mongo.find(any(Query.class),eq(Pedido.class))).thenReturn(pedidos);
        var d=service.consultar(inicio,fim,null,null,"dia",5,2);
        assertEquals(3,d.pedidos().itens().size());assertEquals(3,d.pedidos().totalPaginas());assertEquals("p20",d.pedidos().itens().getFirst().id());
    }
}
