package com.montanaritech.contable.maestros.jurisdiccion;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * Molde F1.8 PL-1. Jurisdicción impositiva: nombre, código, alícuota IIBB, activo.
 */
@Entity
@Table(name = "jurisdiccion", uniqueConstraints = @UniqueConstraint(name = "uk_jurisdiccion_tenant_codigo", columnNames = {"tenant_id", "codigo"}))
@Getter
@Setter
public class Jurisdiccion extends EntidadNegocio {

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(name = "alicuota_iibb", nullable = false, precision = 5, scale = 2)
    private BigDecimal alicuotaIIBB;

    @Column(nullable = false)
    private boolean activo = true;
}
