package com.montanaritech.contable.maestros.proyecto.presupuesto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.montanaritech.contable.common.tenant.EntidadNegocio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * Costo de producción del presupuesto (F2.6): línea libre (nombre + importe
 * USD), no roles fijos — en la planilla real varían proyecto a proyecto
 * (Desarrolladores/Pentesting/Tester QA en un caso, solo "Desarrolladores"
 * en otro).
 */
@Entity
@Table(name = "presupuesto_linea_costo")
@Getter
@Setter
public class PresupuestoLineaCosto extends EntidadNegocio {

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "presupuesto_proyecto_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_presupuesto_linea_costo_presupuesto"))
    private PresupuestoProyecto presupuestoProyecto;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(name = "importe_usd", nullable = false, precision = 18, scale = 2)
    private BigDecimal importeUsd;

    @Column(nullable = false)
    private int orden;
}
