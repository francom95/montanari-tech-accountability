package com.montanaritech.contable.impuestos.iva;

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
 * Un renglón de la liquidación de IVA (F6.1 §1.5). {@code importeCalculado} y
 * {@code importeAjuste} se guardan separados a propósito: es lo que hace
 * auditable el ajuste manual — queda registrado qué dijo el sistema y qué
 * decidió la persona, no solo el resultado final.
 */
@Entity
@Table(name = "liquidacion_iva_componente")
@Getter
@Setter
public class LiquidacionIvaComponente extends EntidadNegocio {

    @ManyToOne(optional = false)
    @JoinColumn(name = "liquidacion_iva_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_liquidacion_iva_componente_liquidacion"))
    private LiquidacionIva liquidacionIva;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoComponenteIva tipo;

    @Column(nullable = false, length = 300)
    private String descripcion;

    @Column(name = "importe_calculado", nullable = false, precision = 18, scale = 2)
    private BigDecimal importeCalculado = BigDecimal.ZERO;

    @Column(name = "importe_ajuste", nullable = false, precision = 18, scale = 2)
    private BigDecimal importeAjuste = BigDecimal.ZERO;

    @Column(name = "motivo_ajuste", length = 500)
    private String motivoAjuste;

    /**
     * Obligatoria solo en los componentes manuales: los automáticos resuelven
     * su cuenta desde {@code tipo.getConcepto()} vía el mapeo de F4.1, así que
     * si el usuario reasigna ese mapeo la liquidación lo sigue sola.
     */
    @ManyToOne(optional = true)
    @JoinColumn(name = "cuenta_contable_id",
            foreignKey = @ForeignKey(name = "fk_liquidacion_iva_componente_cuenta"))
    private CuentaContable cuentaContable;

    @Column(nullable = false)
    private boolean manual = false;

    @Column(nullable = false)
    private Integer orden;

    /** Lo que efectivamente entra al resultado. */
    public BigDecimal getImporteFinal() {
        return importeCalculado.add(importeAjuste);
    }

    /** Aporte con signo al resultado de la liquidación (F6.1 §1.3). */
    public BigDecimal getAporte() {
        return getImporteFinal().multiply(BigDecimal.valueOf(tipo.getSigno()));
    }
}
