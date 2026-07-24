package com.montanaritech.contable.maestros.proyecto.presupuesto;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * Alícuotas y esquema de comisión bancaria COMEX del presupuesto de proyecto
 * (F2.6), una sola fila por tenant, editable por admin — nada de esto se
 * hardcodea en {@link CalculoPresupuestoProyecto} (la planilla real del
 * contador usa estos mismos porcentajes; ver memoria del paso).
 */
@Entity
@Table(name = "configuracion_presupuesto",
        uniqueConstraints = @UniqueConstraint(name = "uk_configuracion_presupuesto_tenant", columnNames = "tenant_id"))
@Getter
@Setter
public class ConfiguracionPresupuesto extends EntidadNegocio {

    @Column(name = "comision_venta_porcentaje", nullable = false, precision = 7, scale = 5)
    private BigDecimal comisionVentaPorcentaje;

    @Column(name = "colchon_impuesto_ganancias_porcentaje", nullable = false, precision = 7, scale = 5)
    private BigDecimal colchonImpuestoGananciasPorcentaje;

    @Column(name = "iibb_convenio_multilateral_porcentaje", nullable = false, precision = 7, scale = 5)
    private BigDecimal iibbConvenioMultilateralPorcentaje;

    @Column(name = "impuesto_debitos_creditos_porcentaje", nullable = false, precision = 7, scale = 5)
    private BigDecimal impuestoDebitosCreditosPorcentaje;

    @Column(name = "iva_porcentaje", nullable = false, precision = 7, scale = 5)
    private BigDecimal ivaPorcentaje;

    @Column(name = "diferencia_dolar_comercializacion_porcentaje", nullable = false, precision = 7, scale = 5)
    private BigDecimal diferenciaDolarComercializacionPorcentaje;

    @Column(name = "percepcion_iva_comex_porcentaje", nullable = false, precision = 7, scale = 5)
    private BigDecimal percepcionIvaComexPorcentaje;

    @Column(name = "iibb_sircreb_comex_porcentaje", nullable = false, precision = 7, scale = 5)
    private BigDecimal iibbSircrebComexPorcentaje;

    @Column(name = "comex_umbral_uno_usd", nullable = false, precision = 18, scale = 2)
    private BigDecimal comexUmbralUnoUsd;

    @Column(name = "comex_monto_uno_usd", nullable = false, precision = 18, scale = 2)
    private BigDecimal comexMontoUnoUsd;

    @Column(name = "comex_umbral_dos_usd", nullable = false, precision = 18, scale = 2)
    private BigDecimal comexUmbralDosUsd;

    @Column(name = "comex_monto_dos_usd", nullable = false, precision = 18, scale = 2)
    private BigDecimal comexMontoDosUsd;

    @Column(name = "comex_umbral_tres_usd", nullable = false, precision = 18, scale = 2)
    private BigDecimal comexUmbralTresUsd;

    @Column(name = "comex_monto_tres_usd", nullable = false, precision = 18, scale = 2)
    private BigDecimal comexMontoTresUsd;

    @Column(name = "comex_porcentaje_excedente", nullable = false, precision = 7, scale = 5)
    private BigDecimal comexPorcentajeExcedente;
}
