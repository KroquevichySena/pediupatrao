package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.NovoPedidoRequest;
import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.MovimentacaoPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoEntrega;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.security.UsuarioLogado;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class PedidoService {

    static final BigDecimal DESCONTO_MAXIMO = new BigDecimal("20");
    static final int QUANTIDADE_MAXIMA_POR_ITEM = 50;
    static final int TAMANHO_MINIMO_JUSTIFICATIVA = 10;
    private static final BigDecimal CEM = new BigDecimal("100");

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final AuditoriaService auditoriaService;
    private final UsuarioLogado usuarioLogado;

    public PedidoService(PedidoRepository pedidoRepository, ClienteRepository clienteRepository,
            ProdutoRepository produtoRepository, AuditoriaService auditoriaService, UsuarioLogado usuarioLogado) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
        this.auditoriaService = auditoriaService;
        this.usuarioLogado = usuarioLogado;
    }

    @PreAuthorize("isAuthenticated()")
    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll(Sort.by(Sort.Direction.DESC, "dataEntrada"));
    }

    @PreAuthorize("isAuthenticated()")
    public Pedido buscarPorId(String id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado: " + id));
    }

    // ========================
    // ENTRADA
    // ========================
    @PreAuthorize("hasRole('ATENDENTE')")
    public Pedido criarPedido(NovoPedidoRequest dados) {
        if (dados == null || dados.clienteId() == null || dados.clienteId().isBlank()) {
            throw new RegraNegocioException("Informe o cliente do pedido.");
        }
        if (dados.tipoEntrega() == null) {
            throw new RegraNegocioException("Informe se o pedido é para entrega ou retirada.");
        }
        if (dados.itens() == null || dados.itens().isEmpty()) {
            throw new RegraNegocioException("O pedido precisa ter pelo menos um item.");
        }

        Cliente cliente = clienteRepository.findById(dados.clienteId())
                .orElseThrow(() -> new RegraNegocioException("Cliente não encontrado."));

        List<ItemPedido> itens = new ArrayList<>();
        for (NovoPedidoRequest.Item item : dados.itens()) {
            itens.add(montarItem(item));
        }

        Pedido pedido = new Pedido();
        pedido.setClienteId(cliente.getId());
        pedido.setClienteNome(cliente.getNome());
        pedido.setTipoEntrega(dados.tipoEntrega());
        pedido.setObservacao(dados.observacao());
        pedido.setItens(itens);
        recalcularValores(pedido);

        LocalDateTime agora = LocalDateTime.now();
        String usuario = usuarioLogado.username();
        pedido.setStatus(StatusPedido.RECEBIDO);
        pedido.setRegistradoPor(usuario);
        pedido.setDataEntrada(agora);
        pedido.getHistorico().add(new MovimentacaoPedido(StatusPedido.RECEBIDO, usuario, agora));

        Pedido salvo = pedidoRepository.save(pedido);

        List<AlteracaoCampo> alteracoes = new ArrayList<>();
        AuditoriaService.comparar(alteracoes, "cliente", null, cliente.getNome());
        AuditoriaService.comparar(alteracoes, "tipoEntrega", null, salvo.getTipoEntrega());
        AuditoriaService.comparar(alteracoes, "itens", null, descreverItens(salvo.getItens()));
        AuditoriaService.comparar(alteracoes, "valorTotal", null, salvo.getValorTotal());
        AuditoriaService.comparar(alteracoes, "status", null, salvo.getStatus());
        AuditoriaService.comparar(alteracoes, "dataEntrada", null, salvo.getDataEntrada());
        auditoriaService.registrar(TipoOperacao.INCLUSAO_PEDIDO, AuditoriaService.ENTIDADE_PEDIDO,
                salvo.getId(), alteracoes, null);
        return salvo;
    }

    private ItemPedido montarItem(NovoPedidoRequest.Item item) {
        if (item == null || item.produtoId() == null || item.produtoId().isBlank()) {
            throw new RegraNegocioException("Item sem produto informado.");
        }
        int quantidade = item.quantidade() == null ? 0 : item.quantidade();
        if (quantidade < 1 || quantidade > QUANTIDADE_MAXIMA_POR_ITEM) {
            throw new RegraNegocioException("Quantidade deve estar entre 1 e " + QUANTIDADE_MAXIMA_POR_ITEM + ".");
        }
        Produto produto = produtoRepository.findById(item.produtoId())
                .orElseThrow(() -> new RegraNegocioException("Produto não encontrado: " + item.produtoId()));
        if (!Boolean.TRUE.equals(produto.getAtivo())) {
            throw new RegraNegocioException("O produto " + produto.getNome() + " está inativo.");
        }
        if (produto.getPreco() == null || produto.getPreco() <= 0) {
            throw new RegraNegocioException("O produto " + produto.getNome() + " está sem preço válido.");
        }
        // O preço vem sempre do cadastro do produto, nunca da requisição
        BigDecimal preco = BigDecimal.valueOf(produto.getPreco()).setScale(2, RoundingMode.HALF_UP);
        return new ItemPedido(produto.getId(), produto.getNome(), quantidade, preco);
    }

    /** Recalcula soma dos itens, desconto e total a partir dos dados já validados. */
    static void recalcularValores(Pedido pedido) {
        BigDecimal valorItens = pedido.getItens().stream()
                .map(ItemPedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal percentual = pedido.getPercentualDesconto() == null ? BigDecimal.ZERO : pedido.getPercentualDesconto();
        BigDecimal valorDesconto = valorItens.multiply(percentual).divide(CEM, 2, RoundingMode.HALF_UP);

        pedido.setValorItens(valorItens);
        pedido.setValorDesconto(valorDesconto);
        pedido.setValorTotal(valorItens.subtract(valorDesconto));
    }

    // ========================
    // TRANSIÇÕES DE STATUS
    // ========================
    @PreAuthorize("hasAnyRole('ATENDENTE','GERENTE')")
    public Pedido atualizarStatus(String id, StatusPedido novoStatus) {
        if (novoStatus == null) {
            throw new RegraNegocioException("Informe o novo status.");
        }
        Pedido pedido = buscarPorId(id);
        StatusPedido atual = pedido.getStatus();

        if (pedido.isCancelado()) {
            throw new RegraNegocioException("Pedido cancelado não pode ter o status alterado.");
        }
        if (novoStatus == StatusPedido.CANCELADO) {
            throw new RegraNegocioException("Use a operação de cancelamento (exclusiva do gerente).");
        }
        if (atual == null || !atual.podeIrPara(novoStatus)) {
            throw new RegraNegocioException("Transição inválida: " + descricao(atual) + " → " + novoStatus.getDescricao() + ".");
        }
        validarTipoDeSaida(pedido, novoStatus);

        LocalDateTime agora = LocalDateTime.now();
        String usuario = usuarioLogado.username();
        List<AlteracaoCampo> alteracoes = new ArrayList<>();
        AuditoriaService.comparar(alteracoes, "status", atual, novoStatus);

        pedido.setStatus(novoStatus);
        pedido.getHistorico().add(new MovimentacaoPedido(novoStatus, usuario, agora));
        if (novoStatus.isSaida()) {
            pedido.setSaidaRegistradaPor(usuario);
            pedido.setDataSaida(agora);
            AuditoriaService.comparar(alteracoes, "saidaRegistradaPor", null, usuario);
            AuditoriaService.comparar(alteracoes, "dataSaida", null, agora);
        }

        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar(TipoOperacao.ALTERACAO_STATUS_PEDIDO, AuditoriaService.ENTIDADE_PEDIDO,
                salvo.getId(), alteracoes, null);
        return salvo;
    }

    private void validarTipoDeSaida(Pedido pedido, StatusPedido novoStatus) {
        TipoEntrega tipo = pedido.getTipoEntrega();
        if (tipo == null) {
            return;
        }
        if (novoStatus == StatusPedido.SAIU_PARA_ENTREGA && tipo != TipoEntrega.ENTREGA) {
            throw new RegraNegocioException("Pedido para retirada não pode sair para entrega.");
        }
        if (novoStatus == StatusPedido.RETIRADO && tipo != TipoEntrega.RETIRADA) {
            throw new RegraNegocioException("Pedido para entrega não pode ser marcado como retirado.");
        }
    }

    // ========================
    // DESCONTO (somente GERENTE)
    // ========================
    @PreAuthorize("hasRole('GERENTE')")
    public Pedido aplicarDesconto(String id, BigDecimal percentual) {
        if (percentual == null) {
            throw new RegraNegocioException("Informe o percentual de desconto.");
        }
        if (percentual.signum() < 0 || percentual.compareTo(DESCONTO_MAXIMO) > 0) {
            throw new RegraNegocioException("O desconto deve estar entre 0% e " + DESCONTO_MAXIMO + "%.");
        }
        Pedido pedido = buscarPorId(id);
        if (pedido.isCancelado()) {
            throw new RegraNegocioException("Pedido cancelado não pode receber desconto.");
        }
        if (pedido.getStatus() == null || !pedido.getStatus().isAntesDaSaida()) {
            throw new RegraNegocioException("Desconto só pode ser aplicado antes da saída para entrega ou retirada.");
        }

        BigDecimal percentualAnterior = pedido.getPercentualDesconto();
        BigDecimal descontoAnterior = pedido.getValorDesconto();
        BigDecimal totalAnterior = pedido.getValorTotal();

        pedido.setPercentualDesconto(percentual.setScale(2, RoundingMode.HALF_UP));
        pedido.setDescontoAplicadoPor(usuarioLogado.username());
        recalcularValores(pedido);

        List<AlteracaoCampo> alteracoes = new ArrayList<>();
        AuditoriaService.comparar(alteracoes, "percentualDesconto", percentualAnterior, pedido.getPercentualDesconto());
        AuditoriaService.comparar(alteracoes, "valorDesconto", descontoAnterior, pedido.getValorDesconto());
        AuditoriaService.comparar(alteracoes, "valorTotal", totalAnterior, pedido.getValorTotal());
        if (alteracoes.isEmpty()) {
            throw new RegraNegocioException("O pedido já está com esse percentual de desconto.");
        }

        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar(TipoOperacao.DESCONTO_PEDIDO, AuditoriaService.ENTIDADE_PEDIDO,
                salvo.getId(), alteracoes, null);
        return salvo;
    }

    // ========================
    // CANCELAMENTO (somente GERENTE)
    // ========================
    @PreAuthorize("hasRole('GERENTE')")
    public Pedido cancelar(String id, String justificativa) {
        String motivo = justificativa == null ? "" : justificativa.trim();
        if (motivo.length() < TAMANHO_MINIMO_JUSTIFICATIVA) {
            throw new RegraNegocioException("A justificativa do cancelamento deve ter pelo menos "
                    + TAMANHO_MINIMO_JUSTIFICATIVA + " caracteres.");
        }
        Pedido pedido = buscarPorId(id);
        if (pedido.isCancelado()) {
            throw new RegraNegocioException("Este pedido já está cancelado.");
        }
        if (pedido.getStatus() == null || !pedido.getStatus().isAntesDaSaida()) {
            throw new RegraNegocioException("Pedido que já saiu para entrega ou retirada não pode ser cancelado.");
        }

        LocalDateTime agora = LocalDateTime.now();
        String usuario = usuarioLogado.username();
        StatusPedido anterior = pedido.getStatus();

        // Itens e valores são mantidos como estavam, apenas o estado muda
        pedido.setStatusAntesCancelamento(anterior);
        pedido.setStatus(StatusPedido.CANCELADO);
        pedido.setCanceladoPor(usuario);
        pedido.setDataCancelamento(agora);
        pedido.setJustificativaCancelamento(motivo);
        pedido.getHistorico().add(new MovimentacaoPedido(StatusPedido.CANCELADO, usuario, agora));

        Pedido salvo = pedidoRepository.save(pedido);

        List<AlteracaoCampo> alteracoes = new ArrayList<>();
        AuditoriaService.comparar(alteracoes, "status", anterior, StatusPedido.CANCELADO);
        AuditoriaService.comparar(alteracoes, "canceladoPor", null, usuario);
        AuditoriaService.comparar(alteracoes, "dataCancelamento", null, agora);
        auditoriaService.registrar(TipoOperacao.CANCELAMENTO_PEDIDO, AuditoriaService.ENTIDADE_PEDIDO,
                salvo.getId(), alteracoes, motivo);
        return salvo;
    }

    private static String descricao(StatusPedido status) {
        return status == null ? "sem status" : status.getDescricao();
    }

    private static String descreverItens(List<ItemPedido> itens) {
        return itens.stream()
                .map(i -> i.getQuantidade() + "x " + i.getNomeProduto() + " (R$ " + i.getPrecoUnitario() + ")")
                .collect(Collectors.joining("; "));
    }
}
