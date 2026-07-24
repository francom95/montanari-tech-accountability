package com.montanaritech.contable.facturacion.cobro;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * Parámetros de mora en cobros (F7.4), una sola fila por tenant, editable
 * por admin. {@code tasaMoraDiariaPorcentaje} en 0 (default) = sin recargo
 * hasta que el admin cargue una tasa real.
 */
@Entity
@Table(name = "configuracion_cobranza",
        uniqueConstraints = @UniqueConstraint(name = "uk_configuracion_cobranza_tenant", columnNames = "tenant_id"))
@Getter
@Setter
public class ConfiguracionCobranza extends EntidadNegocio {

    @Column(name = "dias_gracia_mora", nullable = false)
    private Integer diasGraciaMora = 3;

    @Column(name = "tasa_mora_diaria_porcentaje", nullable = false, precision = 9, scale = 7)
    private BigDecimal tasaMoraDiariaPorcentaje = BigDecimal.ZERO;
}
