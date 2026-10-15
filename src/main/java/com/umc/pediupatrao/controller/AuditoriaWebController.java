package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.service.AuditoriaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuditoriaWebController {

    private final AuditoriaService auditoriaService;

    public AuditoriaWebController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping("/auditoria")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public String listar(@RequestParam(required = false) String entidade,
            @RequestParam(required = false) String entidadeId, Model model) {
        model.addAttribute("registros", auditoriaService.consultar(entidade, entidadeId));
        model.addAttribute("entidade", entidade);
        model.addAttribute("entidadeId", entidadeId);
        model.addAttribute("content", "auditoria/lista :: content");
        return "layout";
    }
}
