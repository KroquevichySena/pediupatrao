package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.ProdutoService;
import com.umc.pediupatrao.service.UsuarioService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {

    private final ClienteService clienteService;
    private final ProdutoService produtoService;
    private final UsuarioService usuarioService;

    public HomeController(ClienteService clienteService, ProdutoService produtoService, UsuarioService usuarioService) {
        this.clienteService = clienteService;
        this.produtoService = produtoService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("content", "home :: content");
        return "layout";
    }

    // ========================
    // USUÁRIOS (ADMIN)
    // ========================
    @GetMapping("/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public String listarUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        model.addAttribute("content", "usuarios/lista :: content");
        return "layout";
    }

    @GetMapping("/usuarios/novo")
    @PreAuthorize("hasRole('ADMIN')")
    public String novoUsuarioForm(Model model) {
        model.addAttribute("usuario", new Usuario());
        model.addAttribute("perfis", Perfil.values());
        model.addAttribute("content", "usuarios/form :: content");
        return "layout";
    }

    @GetMapping("/usuarios/editar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String editarUsuarioForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        return usuarioService.buscarPorId(id).map(u -> {
            u.setPassword(null); // o hash nunca vai para a tela
            model.addAttribute("usuario", u);
            model.addAttribute("perfis", Perfil.values());
            model.addAttribute("content", "usuarios/form :: content");
            return "layout";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("erro", "Usuário não encontrado.");
            return "redirect:/usuarios";
        });
    }

    @PostMapping("/usuarios/salvar")
    @PreAuthorize("hasRole('ADMIN')")
    public String salvarUsuario(@ModelAttribute Usuario usuario, RedirectAttributes redirectAttributes) {
        try {
            usuarioService.salvarUsuario(usuario);
            redirectAttributes.addFlashAttribute("sucesso", "Usuário criado com sucesso!");
        } catch (RegraNegocioException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            return "redirect:/usuarios/novo";
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/editar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String atualizarUsuario(@PathVariable String id, @ModelAttribute Usuario usuario,
            RedirectAttributes redirectAttributes) {
        try {
            usuarioService.atualizarUsuario(id, usuario);
            redirectAttributes.addFlashAttribute("sucesso", "Usuário atualizado com sucesso!");
        } catch (RegraNegocioException | RecursoNaoEncontradoException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            return "redirect:/usuarios/editar/" + id;
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/deletar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deletarUsuario(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            usuarioService.deletarUsuario(id);
            redirectAttributes.addFlashAttribute("sucesso", "Usuário removido com sucesso!");
        } catch (RegraNegocioException | RecursoNaoEncontradoException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/usuarios";
    }

    // ========================
    // PRODUTOS (ADMIN e GERENTE)
    // ========================
    @GetMapping("/produtos")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public String produtos(Model model) {
        model.addAttribute("produtos", produtoService.listarProdutos());
        model.addAttribute("produto", new Produto());
        model.addAttribute("content", "produtos/lista :: content");
        return "layout";
    }

    @PostMapping("/produtos/salvar")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public String salvarProduto(@ModelAttribute Produto produto, RedirectAttributes redirectAttributes) {
        try {
            produto.setId(null);
            produtoService.salvar(produto);
            redirectAttributes.addFlashAttribute("sucesso", "Produto cadastrado!");
        } catch (RegraNegocioException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/produtos";
    }

    @PostMapping("/produtos/{id}/ativo")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public String alternarProduto(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            produtoService.alternarAtivo(id);
        } catch (RecursoNaoEncontradoException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/produtos";
    }

    // ========================
    // CLIENTES
    // ========================
    @GetMapping("/clientes")
    @PreAuthorize("hasAnyRole('ATENDENTE','GERENTE')")
    public String clientes(Model model) {
        model.addAttribute("content", "clientes/lista :: content");
        return "layout";
    }

    @GetMapping("/clientes/novo")
    @PreAuthorize("hasRole('ATENDENTE')")
    public String novoClienteForm(Model model) {
        model.addAttribute("cliente", new Cliente());
        model.addAttribute("content", "clientes/form :: content");
        return "layout";
    }

    @PostMapping("/clientes/salvar")
    @PreAuthorize("hasRole('ATENDENTE')")
    public String salvarCliente(@ModelAttribute Cliente cliente, Model model, RedirectAttributes redirectAttributes) {
        try {
            clienteService.criar(cliente);
        } catch (RegraNegocioException e) {
            return reexibirFormularioCliente(cliente, e.getMessage(), model);
        }
        redirectAttributes.addFlashAttribute("sucesso", "Cliente criado com sucesso!");
        return "redirect:/clientes";
    }

    @GetMapping("/clientes/editar/{id}")
    @PreAuthorize("hasRole('ATENDENTE')")
    public String editarClienteForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        return clienteService.buscarPorId(id).map(c -> {
            model.addAttribute("cliente", c);
            model.addAttribute("content", "clientes/form :: content");
            return "layout";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("erro", "Cliente não encontrado.");
            return "redirect:/clientes";
        });
    }

    @PostMapping("/clientes/editar/{id}")
    @PreAuthorize("hasRole('ATENDENTE')")
    public String atualizarCliente(@PathVariable String id, @ModelAttribute Cliente cliente, Model model,
            RedirectAttributes redirectAttributes) {
        try {
            clienteService.atualizar(id, cliente);
        } catch (RegraNegocioException e) {
            cliente.setId(id);
            return reexibirFormularioCliente(cliente, e.getMessage(), model);
        } catch (RecursoNaoEncontradoException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            return "redirect:/clientes";
        }
        redirectAttributes.addFlashAttribute("sucesso", "Cliente atualizado com sucesso!");
        return "redirect:/clientes";
    }

    private String reexibirFormularioCliente(Cliente cliente, String erro, Model model) {
        model.addAttribute("cliente", cliente);
        model.addAttribute("erro", erro);
        model.addAttribute("content", "clientes/form :: content");
        return "layout";
    }

    // ========================
    // OUTRAS ROTAS
    // ========================
    @GetMapping("/configuracoes")
    public String configuracoes(Model model) {
        model.addAttribute("content", "configuracoes :: content");
        return "layout";
    }

    @GetMapping("/admin")
    public String admin() {
        return "admin";
    }
}
