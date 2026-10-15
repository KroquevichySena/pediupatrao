package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.RegistroAuditoria;
import java.util.List;
import org.springframework.data.repository.Repository;

/**
 * Repositório "somente inclusão": estende Repository (e não MongoRepository) de propósito,
 * para que não existam métodos de exclusão ou atualização disponíveis para a aplicação.
 */
public interface RegistroAuditoriaRepository extends Repository<RegistroAuditoria, String> {

    RegistroAuditoria insert(RegistroAuditoria registro);

    List<RegistroAuditoria> findTop200ByOrderByDataHoraDesc();

    List<RegistroAuditoria> findTop200ByEntidadeOrderByDataHoraDesc(String entidade);

    List<RegistroAuditoria> findByEntidadeAndEntidadeIdOrderByDataHoraDesc(String entidade, String entidadeId);
}
