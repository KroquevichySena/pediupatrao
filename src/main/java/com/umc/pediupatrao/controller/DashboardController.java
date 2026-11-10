package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.DashboardResponse;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoEntrega;
import com.umc.pediupatrao.service.DashboardService;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
public class DashboardController {
    private final DashboardService service;
    public DashboardController(DashboardService service) { this.service = service; }

    @GetMapping
    public DashboardResponse consultar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) TipoEntrega tipoEntrega,
            @RequestParam(defaultValue = "dia") String agrupamento,
            @RequestParam(defaultValue = "5") int limite,
            @RequestParam(defaultValue = "0") int pagina) {
        return service.consultar(inicio, fim, status, tipoEntrega, agrupamento, limite, pagina);
    }

    @GetMapping("/filtros")
    public Map<String, Object> filtros() {
        return Map.of("status", Arrays.stream(StatusPedido.values()).map(s -> Map.of("valor", s.name(), "descricao", s.getDescricao())).toList(),
                "tiposEntrega", TipoEntrega.values(), "agrupamentos", List.of("dia", "mes"), "limiteMaximo", 20, "periodoMaximoDias", 366);
    }
}
