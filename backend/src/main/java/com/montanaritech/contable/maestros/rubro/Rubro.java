package com.montanaritech.contable.maestros.rubro;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.maestros.categoria.Categoria;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Molde F1.8 PL-1. Rubro: nombre, categoría (FK), orden, activo.
 */
@Entity
@Table(name = "rubro", uniqueConstraints = @UniqueConstraint(name = "uk_rubro_tenant_nombre", columnNames = {"tenant_id", "nombre"}))
@Getter
@Setter
public class Rubro extends EntidadNegocio {

    @Column(nullable = false, length = 80)
    private String nombre;

    @ManyToOne(optional = false)
    @JoinColumn(name = "categoria_id", nullable = false, foreignKey = @ForeignKey(name = "fk_rubro_categoria"))
    private Categoria categoria;

    @Column(nullable = false)
    private int orden = 0;

    @Column(nullable = false)
    private boolean activo = true;
}
