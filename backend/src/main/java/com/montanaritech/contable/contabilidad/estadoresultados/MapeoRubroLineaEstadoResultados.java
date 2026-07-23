package com.montanaritech.contable.contabilidad.estadoresultados;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.maestros.categoria.Categoria;
import com.montanaritech.contable.maestros.rubro.Rubro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Mapeo rubro→línea del estado de resultados (F7.3), editable por admin. La
 * clave es (rubro, naturaleza) y no solo rubro: un rubro puede agrupar
 * cuentas RP y RN mezcladas (p. ej. "Otros Ingresos y Egresos"), y solo la
 * naturaleza permite separarlas en líneas distintas sin tocar el plan de
 * cuentas. {@code naturaleza} solo tiene sentido en {@code RP}/{@code RN}
 * (las únicas naturalezas que entran al estado de resultados).
 */
@Entity
@Table(name = "mapeo_rubro_linea_er", uniqueConstraints = @UniqueConstraint(
        name = "uk_mapeo_rubro_linea_er", columnNames = {"tenant_id", "rubro_id", "naturaleza"}))
@Getter
@Setter
public class MapeoRubroLineaEstadoResultados extends EntidadNegocio {

    @ManyToOne(optional = false)
    @JoinColumn(name = "rubro_id", nullable = false, foreignKey = @ForeignKey(name = "fk_mapeo_rubro_linea_er_rubro"))
    private Rubro rubro;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Categoria.TipoCategoria naturaleza;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private LineaEstadoResultados linea;
}
