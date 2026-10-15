package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.NovoPedidoRequest;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoEntrega;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.service.AuditoriaService;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.service.ProdutoService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Telas de pedidos. As regras ficam no PedidoService; aqui só convertemos o formulário. */
@Controller
@RequestMapping("/pedidos")
public class PedidoWebController {

    private final PedidoService pedidoService;
    private final ClienteService clienteService;
    private final ProdutoService produtoService;
    private final AuditoriaService auditoriaService;

    public PedidoWebController(PedidoService pedidoService, ClienteService clienteService,
            ProdutoService produtoService, AuditoriaService auditoriaService) {
        this.pedidoService = pedidoService;
        this.clienteService = clienteService;
        this.produtoService = produtoService;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','ATENDENTE')")
    public String listar(Model model) {
        model.addAttribute("pedidos", pedidoService.listarPedidos());
        model.addAttribute("content", "pedidos :: content");
        return "layout";
    }

    @GetMapping("/novo")
    @PreAuthorize("hasRole('ATENDENTE')")
    public String novo(Model model) {
        model.addAttribute("clientes", clienteService.listarClientes());
        model.addAttribute("produtos", produtoService.listarAtivos());
        model.addAttribute("tiposEntrega", TipoEntrega.values());
        model.addAttribute("content", "pedidos/form :: content");
        return "layout";
    }

    @PostMapping
    @PreAuthorize("hasRole('ATENDENTE')")
    public String registrar(@RequestParam(required = false) String clienteId,
            @RequestParam(required = false) TipoEntrega tipoEntrega,
            @RequestParam(required = false) String observacao,
            @RequestParam(name = "produtoId", required = false) List<String> produtoIds,
            @RequestParam(name = "quantidade", required = false) List<Integer> quantidades,
            RedirectAttributes redirectAttributes) {
        List<NovoPedidoRequest.Item> itens = new ArrayList<>();
        if (produtoIds != null) {
            for (int i = 0; i < produtoIds.size(); i++) {
                if (produtoIds.get(i) == null || produtoIds.get(i).isBlank()) {
                    continue; // linha vazia do formulário
                }
                Integer qtd = quantidades != null && i < quantidades.size() ? quantidades.get(i) : null;
                itens.add(new NovoPedidoRequest.Item(produtoIds.get(i), qtd));
            }
        }
        try {
            Pedido pedido = pedidoService.criarPedido(new NovoPedidoRequest(clienteId, tipoEntrega, observacao, itens));
            redirectAttributes.addFlashAttribute("sucesso", "Pedido registrado!");
            return "redirect:/pedidos/" + pedido.getId();
        } catch (RegraNegocioException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            return "redirect:/pedidos/novo";
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','ATENDENTE')")
    public String detalhe(@PathVariable String id, Model model, Authentication auth,
            RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("pedido", pedidoService.buscarPorId(id));
        } catch (RecursoNaoEncontradoException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            return "redirect:/pedidos";
        }
        boolean podeVerAuditoria = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_GERENTE"));
        if (podeVerAuditoria) {
            model.addAttribute("auditoria", auditoriaService.consultar(AuditoriaService.ENTIDADE_PEDIDO, id));
        }
        model.addAttribute("content", "pedidos/detalhe :: content");
        return "layout";
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ATENDENTE','GERENTE')")
    public String mudarStatus(@PathVariable String id, @RequestParam StatusPedido status,
            RedirectAttributes redirectAttributes) {
        return executar(id, redirectAttributes, "Status atualizado para " + status.getDescricao() + ".",
                () -> pedidoService.atualizarStatus(id, status));
    }

    @PostMapping("/{id}/desconto")
    @PreAuthorize("hasRole('GERENTE')")
    public String desconto(@PathVariable String id, @RequestParam(required = false) BigDecimal percentual,
            RedirectAttributes redirectAttributes) {
        return executar(id, redirectAttributes, "Desconto aplicado.",
                () -> pedidoService.aplicarDesconto(id, percentual));
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('GERENTE')")
    public String cancelar(@PathVariable String id, @RequestParam(required = false) String justificativa,
            RedirectAttributes redirectAttributes) {
        return executar(id, redirectAttributes, "Pedido cancelado.",
                () -> pedidoService.cancelar(id, justificativa));
    }

    private String executar(String id, RedirectAttributes redirectAttributes, String mensagemSucesso, Runnable acao) {
        try {
            acao.run();
            redirectAttributes.addFlashAttribute("sucesso", mensagemSucesso);
        } catch (RegraNegocioException | RecursoNaoEncontradoException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/pedidos/" + id;
    }
}
