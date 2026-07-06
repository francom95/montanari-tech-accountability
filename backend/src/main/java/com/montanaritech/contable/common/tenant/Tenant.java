package com.montanaritech.contable.common.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * La entidad que define un tenant no tiene, ella misma, {@code tenant_id}
 * (F1.1 §2, excepción junto con {@code moneda}). La fila 1 es Montanari Tech
 * (seed en V1__core_tenant_usuario_auditoria.sql).
 */
@Entity
@Table(name = "tenant")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(length = 20)
    private String cuit;

    @Column(nullable = false)
    private boolean activo = true;

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
}
