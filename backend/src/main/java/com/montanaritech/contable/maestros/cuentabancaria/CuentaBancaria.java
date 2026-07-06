package com.montanaritech.contable.maestros.cuentabancaria;

import com.montanaritech.contable.common.saldo.CuentaConSaldo;
import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.maestros.moneda.Moneda;
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
 * Molde F1.8 PL-1 + lógica de saldo inicial (F2.4). Cuenta bancaria / cuenta de
 * dinero: entidad, alias, moneda (FK), tipo, estado de conciliación, saldo
 * inicial con fecha (recalculado vía {@link com.montanaritech.contable.common.saldo.RecalculoSaldoService}), activo.
 */
@Entity
@Table(name = "cuenta_bancaria", uniqueConstraints = @UniqueConstraint(name = "uk_cuenta_bancaria_tenant_alias", columnNames = {"tenant_id", "alias"}))
@Getter
@Setter
public class CuentaBancaria extends EntidadNegocio implements CuentaConSaldo {

    @Column(nullable = false, length = 80)
    private String entidad;

    @Column(nullable = false, length = 80)
    private String alias;

    @ManyToOne(optional = false)
    @JoinColumn(name = "moneda_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cuenta_bancaria_moneda"))
    private Moneda moneda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCuenta tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_conciliacion", nullable = false, length = 20)
    private EstadoConciliacion estadoConciliacion = EstadoConciliacion.PENDIENTE;

    @Column(name = "saldo_inicial", nullable = false, precision = 18, scale = 2)
    private BigDecimal saldoInicial;

    @Column(name = "fecha_saldo_inicial", nullable = false)
    private LocalDate fechaSaldoInicial;

    @Column(name = "saldo_actual", nullable = false, precision = 18, scale = 2)
    private BigDecimal saldoActual;

    @Column(nullable = false)
    private boolean activo = true;

    public enum TipoCuenta {
        CUENTA_CORRIENTE,
        CAJA_AHORRO,
        MERCADO_PAGO,
        CAJA_FISICA,
        OTRA
    }

    public enum EstadoConciliacion {
        CONCILIADA,
        PENDIENTE
    }
}
