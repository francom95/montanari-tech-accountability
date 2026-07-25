package com.montanaritech.contable.alerta;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Parámetros del motor de alertas (F9.1), una sola fila por tenant, editable
 * por admin — mismo molde que {@code ConfiguracionDashboard} (F7.5).
 */
@Entity
@Table(name = "configuracion_alertas",
        uniqueConstraints = @UniqueConstraint(name = "uk_configuracion_alertas_tenant", columnNames = "tenant_id"))
@Getter
@Setter
public class ConfiguracionAlertas extends EntidadNegocio {

    @Column(name = "dias_anticipacion", nullable = false)
    private Integer diasAnticipacion = 7;

    @Column(name = "dias_atraso_cxc", nullable = false)
    private Integer diasAtrasoCxc = 0;
}
