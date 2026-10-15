package com.umc.pediupatrao.entity;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Evento de auditoria. Uma operação gera um único registro, com a lista de campos alterados.
 * Não há setters: depois de criado, o registro não é modificado pela aplicação.
 */
@Document(collection = "auditoria")
public class RegistroAuditoria {

    @Id
    private String id;
    private String usuario;
    private String perfil;
    @Indexed
    private LocalDateTime dataHora;
    private TipoOperacao operacao;
    @Indexed
    private String entidade;
    @Indexed
    private String entidadeId;
    private List<AlteracaoCampo> alteracoes;
    private String justificativa;

    @PersistenceCreator
    RegistroAuditoria(String id, String usuario, String perfil, LocalDateTime dataHora, TipoOperacao operacao,
            String entidade, String entidadeId, List<AlteracaoCampo> alteracoes, String justificativa) {
        this.id = id;
        this.usuario = usuario;
        this.perfil = perfil;
        this.dataHora = dataHora;
        this.operacao = operacao;
        this.entidade = entidade;
        this.entidadeId = entidadeId;
        this.alteracoes = alteracoes == null ? List.of() : List.copyOf(alteracoes);
        this.justificativa = justificativa;
    }

    public RegistroAuditoria(String usuario, String perfil, LocalDateTime dataHora, TipoOperacao operacao,
            String entidade, String entidadeId, List<AlteracaoCampo> alteracoes, String justificativa) {
        this(null, usuario, perfil, dataHora, operacao, entidade, entidadeId, alteracoes, justificativa);
    }

    /** Cópia do registro com outra lista de alterações (usada para exibir dados mascarados). */
    public RegistroAuditoria comAlteracoes(List<AlteracaoCampo> outrasAlteracoes) {
        return new RegistroAuditoria(id, usuario, perfil, dataHora, operacao, entidade, entidadeId,
                outrasAlteracoes, justificativa);
    }

    public String getId() { return id; }
    public String getUsuario() { return usuario; }
    public String getPerfil() { return perfil; }
    public LocalDateTime getDataHora() { return dataHora; }
    public TipoOperacao getOperacao() { return operacao; }
    public String getEntidade() { return entidade; }
    public String getEntidadeId() { return entidadeId; }
    public List<AlteracaoCampo> getAlteracoes() { return alteracoes; }
    public String getJustificativa() { return justificativa; }
}
