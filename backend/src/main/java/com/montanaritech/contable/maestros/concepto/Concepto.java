package com.montanaritech.contable.maestros.concepto;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.maestros.moneda.Moneda;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * Molde F1.8 PL-1. Concepto recurrente: nombre, descripción, cuenta_sugerida, periodicidad, importe, moneda (FK), activo.
 */
@Entity
@Table(name = "concepto_recurrente", uniqueConstraints = @UniqueConstraint(name = "uk_concepto_tenant_nombre", columnNames = {"tenant_id", "nombre"}))
@Getter
@Setter
public class Concepto extends EntidadNegocio {

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(length = 20)
    private String cuentaSugerida;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Periodicidad periodicidad = Periodicidad.UNICA;

    @Column(precision = 19, scale = 2)
    private BigDecimal importe;

    @ManyToOne
    @JoinColumn(name = "moneda_id", foreignKey = @ForeignKey(name = "fk_concepto_moneda"))
    private Moneda moneda;

    @Column(nullable = false)
    private boolean activo = true;
}
