package com.montanaritech.contable.impuestos.iibb;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.maestros.jurisdiccion.Jurisdiccion;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Sub-liquidación de IIBB de una jurisdicción (F6.2 §1.1). El impuesto
 * determinado es un campo (no un componente): sale de {@code baseImponible ×
 * alícuota}, y {@code baseImponible = baseTotal × coeficiente}. El
 * {@code coeficiente} es el dato de Convenio Multilateral —editable, con default
 * por participación de destino— y la {@code alicuota} es un snapshot del maestro
 * al crear. Los {@link LiquidacionIibbComponente} son las deducciones y ajustes.
 */
@Entity
@Table(name = "liquidacion_iibb_jurisdiccion")
@Getter
@Setter
public class LiquidacionIibbJurisdiccion extends EntidadNegocio {

    @ManyToOne(optional = false)
    @JoinColumn(name = "liquidacion_iibb_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_liquidacion_iibb_jur_liquidacion"))
    private LiquidacionIibb liquidacionIibb;

    @ManyToOne(optional = false)
    @JoinColumn(name = "jurisdiccion_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_liquidacion_iibb_jur_jurisdiccion"))
    private Jurisdiccion jurisdiccion;

    /** Coeficiente unificado de Convenio Multilateral (0..1). Default = participación por destino. */
    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal coeficiente = BigDecimal.ZERO;

    @Column(name = "base_imponible", nullable = false, precision = 18, scale = 2)
    private BigDecimal baseImponible = BigDecimal.ZERO;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal alicuota = BigDecimal.ZERO;

    @Column(name = "impuesto_determinado", nullable = false, precision = 18, scale = 2)
    private BigDecimal impuestoDeterminado = BigDecimal.ZERO;

    @Column(name = "saldo_a_pagar", nullable = false, precision = 18, scale = 2)
    private BigDecimal saldoAPagar = BigDecimal.ZERO;

    @Column(name = "saldo_a_favor", nullable = false, precision = 18, scale = 2)
    private BigDecimal saldoAFavor = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer orden;

    @OneToMany(mappedBy = "liquidacionIibbJurisdiccion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<LiquidacionIibbComponente> componentes = new ArrayList<>();
}
