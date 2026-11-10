package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.DashboardResponse;
import com.umc.pediupatrao.dto.DashboardResponse.*;
import com.umc.pediupatrao.entity.*;
import com.umc.pediupatrao.exception.RegraNegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
public class DashboardService {
    private final MongoTemplate mongo;
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    public DashboardService(MongoTemplate mongo) { this.mongo = mongo; }

    public DashboardResponse consultar(LocalDate inicio, LocalDate fim, StatusPedido status,
            TipoEntrega tipo, String agrupamento, int limite, int pagina) {
        LocalDate hoje = LocalDate.now(FUSO);
        if (inicio == null) inicio = hoje.withDayOfMonth(1);
        if (fim == null) fim = hoje;
        long dias = ChronoUnit.DAYS.between(inicio, fim);
        if (dias < 0 || dias > 365) throw new RegraNegocioException("Informe um período de 1 a 366 dias, com início até o fim.");
        if (!Set.of("dia", "mes").contains(agrupamento)) throw new RegraNegocioException("Agrupamento deve ser dia ou mes.");
        if (limite < 1 || limite > 20) throw new RegraNegocioException("Limite deve estar entre 1 e 20.");
        if (pagina < 0 || pagina > 100000) throw new RegraNegocioException("Página deve estar entre 0 e 100000.");

        // O filtro é executado no MongoDB. Dados pessoais e histórico não são carregados.
        Criteria criterio = Criteria.where("dataEntrada").gte(inicio.atStartOfDay()).lt(fim.plusDays(1).atStartOfDay());
        if (status != null) criterio.and("status").is(status);
        if (tipo != null) criterio.and("tipoEntrega").is(tipo);
        Query query = Query.query(criterio).with(Sort.by(Sort.Direction.DESC, "dataEntrada", "id"));
        query.fields().include("dataEntrada").include("status").include("tipoEntrega")
                .include("valorTotal").include("valorDesconto").include("itens");
        List<Pedido> pedidos = mongo.find(query, Pedido.class);

        Map<String, BigDecimal> receitas = new TreeMap<>();
        Map<String, Long> contagens = new TreeMap<>();
        for (LocalDate data = inicio; !data.isAfter(fim); data = data.plusDays(1)) {
            String chave = chave(data, agrupamento);
            receitas.putIfAbsent(chave, BigDecimal.ZERO);
            contagens.putIfAbsent(chave, 0L);
        }
        Map<StatusPedido, Long> situacoes = new EnumMap<>(StatusPedido.class);
        Map<String, ProdutoVendido> ranking = new HashMap<>();
        BigDecimal faturamento = BigDecimal.ZERO, descontos = BigDecimal.ZERO;
        long finalizados = 0, cancelados = 0;
        for (Pedido p : pedidos) {
            if (p.getStatus() != null) situacoes.merge(p.getStatus(), 1L, Long::sum);
            if (p.isCancelado()) cancelados++;
            if (p.getStatus() != StatusPedido.FINALIZADO) continue;
            finalizados++;
            BigDecimal valor = valor(p.getValorTotal());
            faturamento = faturamento.add(valor);
            descontos = descontos.add(valor(p.getValorDesconto()));
            String chave = chave(p.getDataEntrada().toLocalDate(), agrupamento);
            receitas.merge(chave, valor, BigDecimal::add);
            contagens.merge(chave, 1L, Long::sum);
            if (p.getItens() == null) continue;
            for (ItemPedido item : p.getItens()) {
                if (item == null || item.getQuantidade() <= 0) continue;
                String id = item.getProdutoId();
                if (id == null || id.isBlank()) continue;
                ProdutoVendido anterior = ranking.get(id);
                ranking.put(id, new ProdutoVendido(id,
                        anterior == null ? Objects.requireNonNullElse(item.getNomeProduto(), "Produto sem nome") : anterior.nome(),
                        (anterior == null ? 0L : anterior.quantidade()) + item.getQuantidade(),
                        (anterior == null ? BigDecimal.ZERO : anterior.valorBruto()).add(valor(item.getSubtotal()))));
            }
        }
        long total = pedidos.size();
        List<Situacao> statusLista = new ArrayList<>();
        for (StatusPedido s : StatusPedido.values()) statusLista.add(new Situacao(s.name(), s.getDescricao(), situacoes.getOrDefault(s, 0L)));
        long semStatus = pedidos.stream().filter(p -> p.getStatus() == null).count();
        if (semStatus > 0) statusLista.add(new Situacao("SEM_STATUS", "Sem status (legado)", semStatus));
        var indicadores = new Indicadores(total, finalizados, total - finalizados - cancelados, cancelados,
                dinheiro(faturamento), dividir(faturamento, finalizados), dinheiro(descontos),
                dividir(BigDecimal.valueOf(cancelados).multiply(BigDecimal.valueOf(100)), total));
        var vendas = receitas.entrySet().stream().map(e -> new Venda(e.getKey(), dinheiro(e.getValue()), contagens.get(e.getKey()))).toList();
        var produtos = ranking.values().stream().sorted(Comparator.comparingLong(ProdutoVendido::quantidade).reversed()
                .thenComparing(ProdutoVendido::produtoId)).limit(limite).toList();
        int tamanho = 10;
        var recentes = pedidos.stream().skip((long) pagina * tamanho).limit(tamanho)
                .map(p -> new PedidoResumo(p.getId(), p.getDataEntrada(), p.getStatus() == null ? "SEM_STATUS" : p.getStatus().name(),
                        p.getTipoEntrega() == null ? "NÃO INFORMADO" : p.getTipoEntrega().name(), dinheiro(valor(p.getValorTotal())))).toList();
        return new DashboardResponse(inicio, fim, LocalDateTime.now(FUSO), indicadores, vendas, statusLista, produtos,
                new Pagina(recentes, pagina, tamanho, total, (int) ((total + tamanho - 1) / tamanho)));
    }
    private static String chave(LocalDate data, String grupo) { return grupo.equals("mes") ? YearMonth.from(data).toString() : data.toString(); }
    private static BigDecimal valor(BigDecimal n) { return n == null ? BigDecimal.ZERO : n; }
    private static BigDecimal dinheiro(BigDecimal n) { return n.setScale(2, RoundingMode.HALF_UP); }
    private static BigDecimal dividir(BigDecimal n, long d) { return d == 0 ? dinheiro(BigDecimal.ZERO) : n.divide(BigDecimal.valueOf(d), 2, RoundingMode.HALF_UP); }
}
