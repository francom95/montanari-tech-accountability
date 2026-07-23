package com.montanaritech.contable.impuestos.atribucion;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.maestros.moneda.Moneda;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Atribución de un impuesto liquidado (IVA o IIBB) a proyectos (F6.3). La
 * liquidación se referencia en forma polimórfica ({@code liquidacionTipo} +
 * {@code liquidacionId}, sin FK). {@code montoTotal} es el impuesto a repartir
 * (el saldo a pagar de la liquidación), siempre en ARS.
 */
@Entity
@Table(name = "atribucion_impuesto",
        uniqueConstraints = @UniqueConstraint(name = "uk_atribucion_impuesto_liquidacion",
                columnNames = {"tenant_id", "liquidacion_tipo", "liquidacion_id"}))
@Getter
@Setter
public class AtribucionImpuesto extends EntidadNegocio {

    @Enumerated(EnumType.STRING)
    @Column(name = "liquidacion_tipo", nullable = false, length = 10)
    private TipoLiquidacion liquidacionTipo;

    @Column(name = "liquidacion_id", nullable = false)
    private Long liquidacionId;

    @Column(nullable = false)
    private Integer anio;

    @Column(nullable = false)
    private Integer mes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CriterioAtribucion criterio;

    @ManyToOne(optional = false)
    @JoinColumn(name = "moneda_id", nullable = false, foreignKey = @ForeignKey(name = "fk_atribucion_impuesto_moneda"))
    private Moneda moneda;

    @Column(name = "tipo_cambio", nullable = false, precision = 19, scale = 6)
    private BigDecimal tipoCambio = BigDecimal.ONE;

    @Column(name = "monto_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal montoTotal = BigDecimal.ZERO;

    @OneToMany(mappedBy = "atribucionImpuesto", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<AtribucionImpuestoLinea> lineas = new ArrayList<>();
}
