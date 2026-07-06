package com.montanaritech.contable.common.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Superclase de toda entidad de negocio (F1.1 §2). Aporta {@code tenant_id}
 * con filtrado Hibernate, timestamps/autor de auditoría genérica y lock
 * optimista. El filtro se habilita por request en
 * {@link TenantFilterInterceptor}; sin esa habilitación, las consultas NO
 * quedan filtradas por tenant (fail-open a propósito en dev temprano, ver
 * nota del interceptor).
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = Long.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public abstract class EntidadNegocio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @CreatedDate
    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @CreatedBy
    @Column(name = "creado_por", updatable = false, length = 120)
    private String creadoPor;

    @LastModifiedDate
    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @LastModifiedBy
    @Column(name = "actualizado_por", length = 120)
    private String actualizadoPor;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    protected void alPersistir() {
        if (tenantId == null) {
            tenantId = TenantContext.getTenantId();
        }
    }
}
