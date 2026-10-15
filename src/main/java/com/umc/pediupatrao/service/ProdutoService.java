package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    public ProdutoService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    @PreAuthorize("isAuthenticated()")
    public List<Produto> listarProdutos() {
        return produtoRepository.findAll();
    }

    @PreAuthorize("isAuthenticated()")
    public List<Produto> listarAtivos() {
        return produtoRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getAtivo()))
                .toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public void excluir(String id) {
        produtoRepository.deleteById(id);
    }

    // Salva um novo produto ou atualiza um existente
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public Produto salvar(Produto produto) {
        if (produto.getNome() == null || produto.getNome().isBlank()) {
            throw new RegraNegocioException("O nome do produto é obrigatório.");
        }
        if (produto.getPreco() == null || produto.getPreco() <= 0) {
            throw new RegraNegocioException("O preço do produto deve ser maior que zero.");
        }
        if (produto.getAtivo() == null) {
            produto.setAtivo(false);
        }
        if (produto.getId() != null && !produto.getId().isBlank() && !produtoRepository.existsById(produto.getId())) {
            throw new RecursoNaoEncontradoException("Produto não encontrado para atualização.");
        }
        return produtoRepository.save(produto);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public Produto alternarAtivo(String id) {
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado."));
        produto.setAtivo(!Boolean.TRUE.equals(produto.getAtivo()));
        return produtoRepository.save(produto);
    }
}
