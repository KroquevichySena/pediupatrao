package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.RegistroAuditoria;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.repository.RegistroAuditoriaRepository;
import com.umc.pediupatrao.security.UsuarioLogado;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class AuditoriaService {

    public static final String ENTIDADE_PEDIDO = "PEDIDO";
    public static final String ENTIDADE_CLIENTE = "CLIENTE";
    public static final String ENTIDADE_USUARIO = "USUARIO";

    // Campos que nunca têm o valor gravado na auditoria
    private static final Set<String> CAMPOS_SENSIVEIS = Set.of("password", "senha", "token");
    private static final String VALOR_PROTEGIDO = "[protegido]";

    // Campos de dados pessoais exibidos de forma mascarada na consulta
    private static final Set<String> CAMPOS_MASCARADOS = Set.of("telefone");

    private final RegistroAuditoriaRepository repository;
    private final UsuarioLogado usuarioLogado;

    public AuditoriaService(RegistroAuditoriaRepository repository, UsuarioLogado usuarioLogado) {
        this.repository = repository;
        this.usuarioLogado = usuarioLogado;
    }

    public RegistroAuditoria registrar(TipoOperacao operacao, String entidade, String entidadeId,
            List<AlteracaoCampo> alteracoes, String justificativa) {
        RegistroAuditoria registro = new RegistroAuditoria(
                usuarioLogado.username(),
                usuarioLogado.perfil(),
                LocalDateTime.now(),
                operacao,
                entidade,
                entidadeId,
                alteracoes.stream().map(AuditoriaService::protegerSensivel).toList(),
                justificativa);
        return repository.insert(registro);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public List<RegistroAuditoria> consultar(String entidade, String entidadeId) {
        List<RegistroAuditoria> registros;
        if (temTexto(entidade) && temTexto(entidadeId)) {
            registros = repository.findByEntidadeAndEntidadeIdOrderByDataHoraDesc(entidade.toUpperCase(), entidadeId);
        } else if (temTexto(entidade)) {
            registros = repository.findTop200ByEntidadeOrderByDataHoraDesc(entidade.toUpperCase());
        } else {
            registros = repository.findTop200ByOrderByDataHoraDesc();
        }
        return registros.stream().map(AuditoriaService::mascararDadosPessoais).toList();
    }

    /** Adiciona na lista um item de alteração somente se o valor realmente mudou. */
    public static void comparar(List<AlteracaoCampo> destino, String campo, Object anterior, Object novo) {
        String antes = formatar(anterior);
        String depois = formatar(novo);
        if (!Objects.equals(antes, depois)) {
            destino.add(new AlteracaoCampo(campo, antes, depois));
        }
    }

    private static String formatar(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof BigDecimal numero) {
            return numero.stripTrailingZeros().toPlainString();
        }
        String texto = valor.toString().trim();
        return texto.isEmpty() ? null : texto;
    }

    private static AlteracaoCampo protegerSensivel(AlteracaoCampo alteracao) {
        if (CAMPOS_SENSIVEIS.contains(alteracao.getCampo().toLowerCase())) {
            return new AlteracaoCampo(alteracao.getCampo(), VALOR_PROTEGIDO, VALOR_PROTEGIDO);
        }
        return alteracao;
    }

    private static RegistroAuditoria mascararDadosPessoais(RegistroAuditoria registro) {
        boolean temCampoMascarado = registro.getAlteracoes().stream()
                .anyMatch(a -> CAMPOS_MASCARADOS.contains(a.getCampo()));
        if (!temCampoMascarado) {
            return registro;
        }
        List<AlteracaoCampo> mascaradas = registro.getAlteracoes().stream()
                .map(a -> CAMPOS_MASCARADOS.contains(a.getCampo())
                        ? new AlteracaoCampo(a.getCampo(), mascarar(a.getValorAnterior()), mascarar(a.getValorNovo()))
                        : a)
                .toList();
        return registro.comAlteracoes(mascaradas);
    }

    /** Mantém só os 4 últimos dígitos: "(11) 99999-1234" vira "*******1234". */
    static String mascarar(String valor) {
        if (valor == null) {
            return null;
        }
        String digitos = valor.replaceAll("\\D", "");
        if (digitos.length() <= 4) {
            return "****";
        }
        return "*".repeat(digitos.length() - 4) + digitos.substring(digitos.length() - 4);
    }

    private static boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
