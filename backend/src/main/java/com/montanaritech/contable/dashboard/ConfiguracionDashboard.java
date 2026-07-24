package com.montanaritech.contable.dashboard;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Parámetros del dashboard (F7.5), una sola fila por tenant, editable por
 * admin. El vencimiento de IVA/IIBB es un día fijo del mes siguiente al
 * período liquidado (regla simple, sin calendario fiscal real); la ventana
 * de "obligaciones próximas" acota qué facturas de compra POR_VENCER se
 * muestran como alerta.
 */
@Entity
@Table(name = "configuracion_dashboard",
        uniqueConstraints = @UniqueConstraint(name = "uk_configuracion_dashboard_tenant", columnNames = "tenant_id"))
@Getter
@Setter
public class ConfiguracionDashboard extends EntidadNegocio {

    @Column(name = "dia_vencimiento_iva", nullable = false)
    private Integer diaVencimientoIva = 20;

    @Column(name = "dia_vencimiento_iibb", nullable = false)
    private Integer diaVencimientoIibb = 15;

    @Column(name = "ventana_obligaciones_dias", nullable = false)
    private Integer ventanaObligacionesDias = 15;
}
