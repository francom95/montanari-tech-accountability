package com.montanaritech.contable.maestros.categoria;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Molde F1.8 PL-1. Categoría contable: nombre, descripción, tipo (Activo/Pasivo/PN/R+/R-), activo.
 */
@Entity
@Table(name = "categoria_contable", uniqueConstraints = @UniqueConstraint(name = "uk_categoria_tenant_nombre", columnNames = {"tenant_id", "nombre"}))
@Getter
@Setter
public class Categoria extends EntidadNegocio {

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCategoria tipo;

    @Column(nullable = false)
    private boolean activo = true;

    public enum TipoCategoria {
        ACTIVO,
        PASIVO,
        PN,
        RPLUS,
        RMINUS
    }
}
