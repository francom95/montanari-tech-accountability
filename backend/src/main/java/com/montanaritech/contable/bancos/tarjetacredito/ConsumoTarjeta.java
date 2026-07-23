package com.montanaritech.contable.bancos.tarjetacredito;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.maestros.concepto.Concepto;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.proveedor.Proveedor;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.tarjetacredito.TarjetaCredito;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Línea de un resumen de tarjeta importado (F5.4, reusa {@code ParserTarjeta}
 * de F5.2): consumo, impuesto/comisión, o devolución. {@code importe} lleva
 * signo (positivo = cargo que aumenta la deuda, negativo = devolución/
 * crédito que la reduce) — misma convención que {@code MovimientoBancario}.
 * Entra siempre sin clasificar ({@code cuentaContable} nula); nunca impacta
 * la contabilidad por sí sola, solo alimenta el saldo de la tarjeta — el
 * asiento se genera recién al pagar el resumen ({@code PagoTarjeta}), no por
 * cada línea individual.
 */
@Entity
@Table(name = "consumo_tarjeta", uniqueConstraints = @UniqueConstraint(name = "uk_consumo_tarjeta_hash", columnNames = {"tarjeta_credito_id", "hash_importacion"}))
@Getter
@Setter
public class ConsumoTarjeta extends EntidadNegocio {

    @ManyToOne(optional = false)
    @JoinColumn(name = "tarjeta_credito_id", nullable = false, foreignKey = @ForeignKey(name = "fk_consumo_tarjeta_tarjeta"))
    private TarjetaCredito tarjetaCredito;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(length = 100)
    private String referencia;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal importe;

    @ManyToOne(optional = false)
    @JoinColumn(name = "moneda_id", nullable = false, foreignKey = @ForeignKey(name = "fk_consumo_tarjeta_moneda"))
    private Moneda moneda;

    @Column(name = "tipo_cambio", nullable = false, precision = 19, scale = 6)
    private BigDecimal tipoCambio;

    @Column(name = "importe_ars", nullable = false, precision = 18, scale = 2)
    private BigDecimal importeArs;

    /** Nula hasta clasificar (manual o por regla, F5.4 §2). */
    @ManyToOne(optional = true)
    @JoinColumn(name = "cuenta_contable_id", foreignKey = @ForeignKey(name = "fk_consumo_tarjeta_cuenta"))
    private CuentaContable cuentaContable;

    @ManyToOne(optional = true)
    @JoinColumn(name = "proveedor_id", foreignKey = @ForeignKey(name = "fk_consumo_tarjeta_proveedor"))
    private Proveedor proveedor;

    @ManyToOne(optional = true)
    @JoinColumn(name = "proyecto_id", foreignKey = @ForeignKey(name = "fk_consumo_tarjeta_proyecto"))
    private Proyecto proyecto;

    /** Asociación opcional a una suscripción/concepto recurrente (F2.1). */
    @ManyToOne(optional = true)
    @JoinColumn(name = "concepto_id", foreignKey = @ForeignKey(name = "fk_consumo_tarjeta_concepto"))
    private Concepto concepto;

    /** Hash de tarjeta+fecha+importe+descripción+referencia (F5.2/F5.4): evita duplicar filas al re-importar el mismo resumen. */
    @Column(name = "hash_importacion", length = 64)
    private String hashImportacion;
}
