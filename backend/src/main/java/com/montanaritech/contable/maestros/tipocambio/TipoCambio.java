package com.montanaritech.contable.maestros.tipocambio;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.maestros.moneda.Moneda;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * Molde F1.8 PL-1. Tipo de cambio: fecha, moneda, criterio (BNA venta/compra, oficial, manual, otro),
 * valor_compra, valor_venta.
 */
@Entity
@Table(name = "tipo_cambio", uniqueConstraints = @UniqueConstraint(name = "uk_tipo_cambio_tenant_fecha_moneda_criterio", columnNames = {"tenant_id", "fecha", "moneda_id", "criterio"}))
@Getter
@Setter
public class TipoCambio extends EntidadNegocio {

    @Column(nullable = false)
    private LocalDate fecha;

    @ManyToOne(optional = false)
    @JoinColumn(name = "moneda_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tipo_cambio_moneda"))
    private Moneda moneda;

    @Column(nullable = false, length = 50)
    private String criterio;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal valorCompra;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal valorVenta;

    @Column(length = 120)
    private String fuente;

    @Column(length = 500)
    private String observaciones;

    @Column(nullable = false)
    private boolean activo = true;
}
