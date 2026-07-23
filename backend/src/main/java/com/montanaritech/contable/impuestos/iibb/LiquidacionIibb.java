package com.montanaritech.contable.impuestos.iibb;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.contabilidad.asiento.Asiento;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Liquidación mensual de IIBB de un período (F6.2, moldes PL-4/PL-5). A
 * diferencia de la de IVA, agrupa una sub-liquidación {@link LiquidacionIibbJurisdiccion}
 * por cada jurisdicción; los saldos de cabecera son la suma informativa de las
 * jurisdicciones. Un único asiento cubre todas.
 */
@Entity
@Table(name = "liquidacion_iibb",
        uniqueConstraints = @UniqueConstraint(name = "uk_liquidacion_iibb_asiento", columnNames = "asiento_id"))
@Getter
@Setter
public class LiquidacionIibb extends EntidadNegocio {

    @Column(nullable = false)
    private Integer anio;

    @Column(nullable = false)
    private Integer mes;

    @Column(name = "fecha_desde", nullable = false)
    private LocalDate fechaDesde;

    @Column(name = "fecha_hasta", nullable = false)
    private LocalDate fechaHasta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoDocumento estado = EstadoDocumento.BORRADOR;

    /** Ventas netas del período repartidas entre las jurisdicciones por coeficiente. */
    @Column(name = "base_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal baseTotal = BigDecimal.ZERO;

    @Column(name = "saldo_a_pagar_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal saldoAPagarTotal = BigDecimal.ZERO;

    @Column(name = "saldo_a_favor_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal saldoAFavorTotal = BigDecimal.ZERO;

    @ManyToOne(optional = true)
    @JoinColumn(name = "asiento_id", foreignKey = @ForeignKey(name = "fk_liquidacion_iibb_asiento"))
    private Asiento asiento;

    @Column(length = 2000)
    private String observaciones;

    @OneToMany(mappedBy = "liquidacionIibb", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<LiquidacionIibbJurisdiccion> jurisdicciones = new ArrayList<>();
}
