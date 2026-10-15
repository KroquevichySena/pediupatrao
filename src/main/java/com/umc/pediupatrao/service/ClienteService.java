package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ClienteRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final AuditoriaService auditoriaService;

    public ClienteService(ClienteRepository clienteRepository, AuditoriaService auditoriaService) {
        this.clienteRepository = clienteRepository;
        this.auditoriaService = auditoriaService;
    }

    @PreAuthorize("hasAnyRole('ATENDENTE','GERENTE')")
    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    @PreAuthorize("hasAnyRole('ATENDENTE','GERENTE')")
    public Optional<Cliente> buscarPorId(String id) {
        return clienteRepository.findById(id);
    }

    /** Mantido por compatibilidade: cria quando não há id e atualiza quando há. */
    @PreAuthorize("hasRole('ATENDENTE')")
    public Cliente salvar(Cliente cliente) {
        if (cliente.getId() == null || cliente.getId().isBlank()) {
            return criar(cliente);
        }
        return atualizar(cliente.getId(), cliente);
    }

    @PreAuthorize("hasRole('ATENDENTE')")
    public Cliente criar(Cliente dados) {
        validar(dados);
        dados.setId(null);
        Cliente salvo = clienteRepository.save(dados);

        List<AlteracaoCampo> alteracoes = new ArrayList<>();
        compararCampos(alteracoes, new Cliente(), salvo);
        auditoriaService.registrar(TipoOperacao.INCLUSAO_CLIENTE, AuditoriaService.ENTIDADE_CLIENTE,
                salvo.getId(), alteracoes, null);
        return salvo;
    }

    @PreAuthorize("hasRole('ATENDENTE')")
    public Cliente atualizar(String id, Cliente dados) {
        validar(dados);
        Cliente existente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado para atualização."));

        List<AlteracaoCampo> alteracoes = new ArrayList<>();
        compararCampos(alteracoes, existente, dados);
        if (alteracoes.isEmpty()) {
            return existente;
        }

        existente.setNome(dados.getNome());
        existente.setTelefone(dados.getTelefone());
        existente.setCep(dados.getCep());
        existente.setLogradouro(dados.getLogradouro());
        existente.setNumero(dados.getNumero());
        existente.setComplemento(dados.getComplemento());
        existente.setBairro(dados.getBairro());
        existente.setCidade(dados.getCidade());
        existente.setEstado(dados.getEstado());
        // o campo "endereco" antigo não é editado pelo formulário e fica preservado

        Cliente salvo = clienteRepository.save(existente);
        auditoriaService.registrar(TipoOperacao.ALTERACAO_CLIENTE, AuditoriaService.ENTIDADE_CLIENTE,
                salvo.getId(), alteracoes, null);
        return salvo;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public void excluir(String id) {
        Cliente existente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado."));
        clienteRepository.deleteById(id);

        // Na exclusão guardamos só a identificação, sem replicar endereço e telefone
        List<AlteracaoCampo> alteracoes = new ArrayList<>();
        AuditoriaService.comparar(alteracoes, "nome", existente.getNome(), null);
        auditoriaService.registrar(TipoOperacao.EXCLUSAO_CLIENTE, AuditoriaService.ENTIDADE_CLIENTE,
                id, alteracoes, null);
    }

    private void validar(Cliente dados) {
        if (dados == null || dados.getNome() == null || dados.getNome().isBlank()) {
            throw new RegraNegocioException("O nome do cliente é obrigatório.");
        }
        String digitos = dados.getTelefone() == null ? "" : dados.getTelefone().replaceAll("\\D", "");
        if (digitos.length() < 10 || digitos.length() > 11) {
            throw new RegraNegocioException("Telefone inválido: informe DDD + número.");
        }
        if (dados.getEstado() != null && !dados.getEstado().isBlank() && dados.getEstado().trim().length() != 2) {
            throw new RegraNegocioException("Estado deve ter 2 letras (ex: SP).");
        }
    }

    private static void compararCampos(List<AlteracaoCampo> destino, Cliente antes, Cliente depois) {
        AuditoriaService.comparar(destino, "nome", antes.getNome(), depois.getNome());
        AuditoriaService.comparar(destino, "telefone", antes.getTelefone(), depois.getTelefone());
        AuditoriaService.comparar(destino, "cep", antes.getCep(), depois.getCep());
        AuditoriaService.comparar(destino, "logradouro", antes.getLogradouro(), depois.getLogradouro());
        AuditoriaService.comparar(destino, "numero", antes.getNumero(), depois.getNumero());
        AuditoriaService.comparar(destino, "complemento", antes.getComplemento(), depois.getComplemento());
        AuditoriaService.comparar(destino, "bairro", antes.getBairro(), depois.getBairro());
        AuditoriaService.comparar(destino, "cidade", antes.getCidade(), depois.getCidade());
        AuditoriaService.comparar(destino, "estado", antes.getEstado(), depois.getEstado());
    }
}
