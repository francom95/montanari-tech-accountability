package com.montanaritech.contable.compromiso;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.proveedor.Proveedor;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * Compromiso de pago futuro (F8.2): egreso presupuestado a nivel plan, previo
 * a convertirse en un {@code Vencimiento} concreto de calendario (F8.1). Un
 * compromiso puede opcionalmente generar su propio vencimiento al crearse
 * ({@code vencimientoGeneradoId}); ambos alimentan el flujo proyectado de F8.3
 * como fuentes paralelas, no uno reemplaza al otro.
 */
@Entity
@Table(name = "compromiso")
@Getter
@Setter
public class Compromiso extends EntidadNegocio {

    @Column(nullable = false, length = 200)
    private String concepto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoCompromiso tipo;

    @Column(name = "fecha_prevista", nullable = false)
    private LocalDate fechaPrevista;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal importe;

    @ManyToOne(optional = false)
    @JoinColumn(name = "moneda_id", foreignKey = @ForeignKey(name = "fk_compromiso_moneda"))
    private Moneda moneda;

    @ManyToOne
    @JoinColumn(name = "proveedor_id", foreignKey = @ForeignKey(name = "fk_compromiso_proveedor"))
    private Proveedor proveedor;

    @ManyToOne
    @JoinColumn(name = "proyecto_id", foreignKey = @ForeignKey(name = "fk_compromiso_proyecto"))
    private Proyecto proyecto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCompromiso estado = EstadoCompromiso.PENDIENTE;

    @Column(length = 500)
    private String observaciones;

    /** Vencimiento generado al crear el compromiso (checkbox), si el usuario lo pidió. */
    @Column(name = "vencimiento_generado_id")
    private Long vencimientoGeneradoId;

    @Column(nullable = false)
    private boolean activo = true;
}
