package com.montanaritech.contable.maestros.moneda;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Entidad ejemplo de la plantilla PL-1 (F1.8): maestro simple, sin estados
 * ni relaciones. Para replicar esta plantilla en una entidad nueva: copiar
 * la forma (extends EntidadNegocio, sin lógica propia acá — toda regla vive
 * en el service) y cambiar los campos propios del dominio.
 */
@Entity
@Table(name = "moneda", uniqueConstraints = @UniqueConstraint(name = "uk_moneda_tenant_codigo", columnNames = {"tenant_id", "codigo"}))
@Getter
@Setter
public class Moneda extends EntidadNegocio {

    /** ISO 4217 (ARS, USD, ...). */
    @Column(nullable = false, length = 3)
    private String codigo;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 5)
    private String simbolo;

    @Column(nullable = false)
    private boolean activo = true;
}
