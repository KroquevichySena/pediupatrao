package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.RegistroAuditoria;
import com.umc.pediupatrao.service.AuditoriaService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** API de auditoria: somente consulta. Não existem endpoints de alteração ou exclusão. */
@RestController
@RequestMapping("/api/auditoria")
@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public List<RegistroAuditoria> consultar(@RequestParam(required = false) String entidade,
            @RequestParam(required = false) String entidadeId) {
        return auditoriaService.consultar(entidade, entidadeId);
    }
}
