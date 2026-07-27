package com.montanaritech.contable.periodo;

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
 * Período contable mensual (F9.3). Las filas nacen on-demand vía
 * {@link PeriodoService#generarAutomaticos()} (mismo molde que
 * {@code VencimientoService.generarAutomaticos()} de F8.1): un (año, mes)
 * sin fila todavía se trata como {@code ABIERTO} implícito (ver
 * {@link PeriodoService#estaCerrado}), así que esta tabla solo contiene
 * meses que alguna vez tuvieron al menos un asiento.
 */
@Entity
@Table(name = "periodo", uniqueConstraints = @UniqueConstraint(name = "uk_periodo_tenant_anio_mes", columnNames = {"tenant_id", "anio", "mes"}))
@Getter
@Setter
public class Periodo extends EntidadNegocio {

    @Column(nullable = false)
    private Integer anio;

    @Column(nullable = false)
    private Integer mes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPeriodo estado = EstadoPeriodo.ABIERTO;

    @Column(name = "motivo_cierre", length = 500)
    private String motivoCierre;

    @Column(name = "motivo_reapertura", length = 500)
    private String motivoReapertura;
}
