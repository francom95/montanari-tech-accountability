package com.montanaritech.contable.maestros.tipocambio;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Criterio de TC por defecto del sistema (F7.4), una sola fila por tenant,
 * editable por admin. {@code criterioPorDefecto} nulo (default) preserva el
 * comportamiento histórico de {@code resolverTipoCambioAutomatico}: toma la
 * primera cotización activa para (moneda, fecha) sin distinguir criterio.
 */
@Entity
@Table(name = "configuracion_tipo_cambio",
        uniqueConstraints = @UniqueConstraint(name = "uk_configuracion_tipo_cambio_tenant", columnNames = "tenant_id"))
@Getter
@Setter
public class ConfiguracionTipoCambio extends EntidadNegocio {

    @Column(name = "criterio_por_defecto", length = 50)
    private String criterioPorDefecto;
}
