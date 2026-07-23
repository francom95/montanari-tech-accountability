package com.montanaritech.contable.impuestos.atribucion;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
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
 * Un renglón de atribución: cuánto del impuesto le toca a un proyecto (F6.3). El
 * {@code porcentaje} es informativo (puede no sumar 100 exacto por redondeo); lo
 * que suma exacto al total es {@code monto}, porque la última línea absorbe el
 * residuo de los redondeos previos.
 */
@Entity
@Table(name = "atribucion_impuesto_linea")
@Getter
@Setter
public class AtribucionImpuestoLinea extends EntidadNegocio {

    @ManyToOne(optional = false)
    @JoinColumn(name = "atribucion_impuesto_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_atribucion_linea_atribucion"))
    private AtribucionImpuesto atribucionImpuesto;

    @ManyToOne(optional = false)
    @JoinColumn(name = "proyecto_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_atribucion_linea_proyecto"))
    private Proyecto proyecto;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal porcentaje = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal monto = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer orden;
}
