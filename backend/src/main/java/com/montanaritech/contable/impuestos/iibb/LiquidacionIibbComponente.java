package com.montanaritech.contable.impuestos.iibb;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * Una deducción o ajuste dentro de una jurisdicción (F6.2 §1.3). Igual patrón
 * auditable que {@code LiquidacionIvaComponente}: {@code importeCalculado} (lo
 * que puso el sistema) y {@code importeAjuste} (lo que decidió la persona) van
 * separados.
 */
@Entity
@Table(name = "liquidacion_iibb_componente")
@Getter
@Setter
public class LiquidacionIibbComponente extends EntidadNegocio {

    @ManyToOne(optional = false)
    @JoinColumn(name = "liquidacion_iibb_jur_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_liquidacion_iibb_comp_jur"))
    private LiquidacionIibbJurisdiccion liquidacionIibbJurisdiccion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoComponenteIibb tipo;

    @Column(nullable = false, length = 300)
    private String descripcion;

    @Column(name = "importe_calculado", nullable = false, precision = 18, scale = 2)
    private BigDecimal importeCalculado = BigDecimal.ZERO;

    @Column(name = "importe_ajuste", nullable = false, precision = 18, scale = 2)
    private BigDecimal importeAjuste = BigDecimal.ZERO;

    @Column(name = "motivo_ajuste", length = 500)
    private String motivoAjuste;

    /** Obligatoria solo en {@code OTRO}; los demás resuelven su cuenta desde {@code tipo.getConcepto()}. */
    @ManyToOne(optional = true)
    @JoinColumn(name = "cuenta_contable_id",
            foreignKey = @ForeignKey(name = "fk_liquidacion_iibb_comp_cuenta"))
    private CuentaContable cuentaContable;

    @Column(nullable = false)
    private boolean manual = false;

    @Column(nullable = false)
    private Integer orden;

    /** Lo que efectivamente entra al resultado de la jurisdicción. */
    public BigDecimal getImporteFinal() {
        return importeCalculado.add(importeAjuste);
    }

    /** Aporte con signo (las deducciones restan). */
    public BigDecimal getAporte() {
        return getImporteFinal().multiply(BigDecimal.valueOf(tipo.getSigno()));
    }
}
