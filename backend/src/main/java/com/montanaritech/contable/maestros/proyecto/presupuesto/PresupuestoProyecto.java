package com.montanaritech.contable.maestros.proyecto.presupuesto;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Presupuesto estimado por proyecto (F2.6), 1:1 con {@link Proyecto}. Se
 * arma en USD y se recalcula en vivo (sin ciclo de estados): los importes
 * derivados (comisión, colchón de IG, impuestos, precio final) NO se
 * persisten acá, {@link CalculoPresupuestoProyecto} los deriva on-demand a
 * partir de estos inputs + {@link ConfiguracionPresupuesto} vigente +
 * {@code Proyecto.tipoProyecto} (Argentina vs Exterior deciden qué cascada
 * de impuestos/comisiones aplica).
 */
@Entity
@Table(name = "presupuesto_proyecto",
        uniqueConstraints = @UniqueConstraint(name = "uk_presupuesto_proyecto_proyecto", columnNames = "proyecto_id"))
@Getter
@Setter
public class PresupuestoProyecto extends EntidadNegocio {

    @OneToOne(optional = false)
    @JoinColumn(name = "proyecto_id", nullable = false, foreignKey = @ForeignKey(name = "fk_presupuesto_proyecto_proyecto"))
    private Proyecto proyecto;

    /** "Ganancia Neta Real Empresa (Margen Deseado)" — lo que la empresa quiere ganar, en USD. */
    @Column(name = "margen_deseado_usd", nullable = false, precision = 18, scale = 2)
    private BigDecimal margenDeseadoUsd;

    /**
     * "Comisiones Bancarias Intermedias de COMEX" — solo aplica a proyectos
     * EXTERIOR; se avería según cliente y monto, por eso es un input manual
     * y no un parámetro de sistema.
     */
    @Column(name = "comisiones_bancarias_intermedias_comex_usd", precision = 18, scale = 2)
    private BigDecimal comisionesBancariasIntermediasComexUsd;

    @Column(length = 2000)
    private String observaciones;

    @OneToMany(mappedBy = "presupuestoProyecto", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<PresupuestoLineaCosto> lineasCosto = new ArrayList<>();
}
