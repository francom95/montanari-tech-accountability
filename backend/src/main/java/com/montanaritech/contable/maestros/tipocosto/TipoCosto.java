package com.montanaritech.contable.maestros.tipocosto;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Molde F1.8 PL-1. Tipo de costo: nombre, descripción, activo.
 */
@Entity
@Table(name = "tipo_costo", uniqueConstraints = @UniqueConstraint(name = "uk_tipo_costo_tenant_nombre", columnNames = {"tenant_id", "nombre"}))
@Getter
@Setter
public class TipoCosto extends EntidadNegocio {

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private boolean activo = true;
}
