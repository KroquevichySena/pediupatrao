package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.*;
import com.umc.pediupatrao.repository.PedidoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Perfil explícito e banco separado: nunca povoa a base normal. IDs tornam o seed idempotente. */
@Configuration
@Profile("demo")
public class DashboardDemoConfig {
    @Bean
    CommandLineRunner pedidosDemonstracao(PedidoRepository repo) {
        return args -> {
            LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
            StatusPedido[] estados = StatusPedido.values();
            for (int i = 0; i < 90; i++) {
                String id = "demo-dashboard-" + hoje + "-" + i;
                if (repo.existsById(id)) continue;
                Pedido p = new Pedido();
                p.setId(id);
                p.setClienteNome("Cliente demonstração");
                p.setTipoEntrega(i % 3 == 0 ? TipoEntrega.RETIRADA : TipoEntrega.ENTREGA);
                p.setStatus(i % 4 == 0 ? estados[i % estados.length] : StatusPedido.FINALIZADO);
                p.setDataEntrada(hoje.minusDays(i % 28).atTime(18 + i % 5, i % 60));
                p.setRegistradoPor("demo");
                ItemPedido pizza = new ItemPedido("demo-pizza-" + i % 3,
                        List.of("Pizza Calabresa", "Pizza Margherita", "Pizza Quatro Queijos").get(i % 3),
                        1 + i % 3, new BigDecimal("54.90"));
                ItemPedido bebida = new ItemPedido("demo-bebida", "Refrigerante 2L", 1, new BigDecimal("14.00"));
                p.setItens(List.of(pizza, bebida));
                p.setValorItens(pizza.getSubtotal().add(bebida.getSubtotal()));
                p.setValorDesconto(i % 5 == 0 ? new BigDecimal("5.00") : BigDecimal.ZERO);
                p.setValorTotal(p.getValorItens().subtract(p.getValorDesconto()));
                repo.save(p);
            }
        };
    }
}
