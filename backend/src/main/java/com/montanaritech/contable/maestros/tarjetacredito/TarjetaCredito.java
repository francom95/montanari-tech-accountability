package com.montanaritech.contable.maestros.tarjetacredito;

import com.montanaritech.contable.common.saldo.CuentaConSaldo;
import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancaria;
import com.montanaritech.contable.maestros.moneda.Moneda;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * Molde F1.8 PL-1 + lógica de saldo inicial (F2.4). Tarjeta de crédito: entidad,
 * moneda (FK), día de cierre/vencimiento, cuenta bancaria de débito (FK), saldo
 * inicial con fecha (recalculado vía {@link com.montanaritech.contable.common.saldo.RecalculoSaldoService}), activo.
 */
@Entity
@Table(name = "tarjeta_credito")
@Getter
@Setter
public class TarjetaCredito extends EntidadNegocio implements CuentaConSaldo {

    @Column(nullable = false, length = 80)
    private String entidad;

    @ManyToOne(optional = false)
    @JoinColumn(name = "moneda_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tarjeta_credito_moneda"))
    private Moneda moneda;

    @Column(name = "dia_cierre", nullable = false)
    private int diaCierre;

    @Column(name = "dia_vencimiento", nullable = false)
    private int diaVencimiento;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cuenta_bancaria_debito_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tarjeta_credito_cuenta_debito"))
    private CuentaBancaria cuentaBancariaDebito;

    /**
     * Cuenta contable pasiva espejo de la deuda con la tarjeta (F5.4, mismo
     * rol que {@code CuentaBancaria.cuentaContable} desde F4.4). Nullable:
     * no hay forma de adivinar un backfill correcto para tarjetas ya
     * creadas antes de este paso — el alta/edición la exige desde ahora,
     * y {@code PagoTarjetaService} rechaza pagar el resumen si falta.
     */
    @ManyToOne(optional = true)
    @JoinColumn(name = "cuenta_contable_id", foreignKey = @ForeignKey(name = "fk_tarjeta_credito_cuenta_contable"))
    private CuentaContable cuentaContable;

    @Column(name = "saldo_inicial", nullable = false, precision = 18, scale = 2)
    private BigDecimal saldoInicial;

    @Column(name = "fecha_saldo_inicial", nullable = false)
    private LocalDate fechaSaldoInicial;

    @Column(name = "saldo_actual", nullable = false, precision = 18, scale = 2)
    private BigDecimal saldoActual;

    @Column(nullable = false)
    private boolean activo = true;
}
