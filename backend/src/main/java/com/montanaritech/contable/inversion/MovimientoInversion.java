package com.montanaritech.contable.inversion;

import com.montanaritech.contable.bancos.movimientobancario.MovimientoBancario;
import com.montanaritech.contable.common.tenant.EntidadNegocio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * Movimiento de una {@link Inversion} (suscripción/rescate, F8.4). Ledger
 * inmutable: sin endpoint de eliminación, mismo criterio que
 * {@code ConsumoTarjeta}. Al crearse genera automáticamente su
 * {@code MovimientoBancario} en la cuenta de origen (F5.1, no F4.4 — no hay
 * asiento contable directo, ver decisión F8.4).
 */
@Entity
@Table(name = "movimiento_inversion",
        uniqueConstraints = @UniqueConstraint(name = "uk_movimiento_inversion_mov_bancario", columnNames = "movimiento_bancario_id"))
@Getter
@Setter
public class MovimientoInversion extends EntidadNegocio {

    @ManyToOne(optional = false)
    @JoinColumn(name = "inversion_id", nullable = false, foreignKey = @ForeignKey(name = "fk_movimiento_inversion_inversion"))
    private Inversion inversion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimientoInversion tipo;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "monto_aplicado", nullable = false, precision = 18, scale = 2)
    private BigDecimal montoAplicado;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal cuotapartes;

    @Column(name = "valor_cuotaparte", nullable = false, precision = 19, scale = 6)
    private BigDecimal valorCuotaparte;

    @Column(name = "fecha_liquidacion")
    private LocalDate fechaLiquidacion;

    @ManyToOne(optional = true)
    @JoinColumn(name = "movimiento_bancario_id", foreignKey = @ForeignKey(name = "fk_movimiento_inversion_mov_bancario"))
    private MovimientoBancario movimientoBancario;

    @Column(length = 500)
    private String observaciones;
}
