package com.montanaritech.contable.impuestos.atribucion;

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
 * Configuración del criterio de prorrateo por defecto del sistema (F6.3), una
 * sola fila por tenant, editable por admin. Cada atribución puede overridear
 * este criterio.
 */
@Entity
@Table(name = "configuracion_atribucion",
        uniqueConstraints = @UniqueConstraint(name = "uk_configuracion_atribucion_tenant", columnNames = "tenant_id"))
@Getter
@Setter
public class ConfiguracionAtribucion extends EntidadNegocio {

    @Enumerated(EnumType.STRING)
    @Column(name = "criterio_por_defecto", nullable = false, length = 30)
    private CriterioAtribucion criterioPorDefecto = CriterioAtribucion.FACTURACION;
}
