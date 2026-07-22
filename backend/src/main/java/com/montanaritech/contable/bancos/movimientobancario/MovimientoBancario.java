package com.montanaritech.contable.bancos.movimientobancario;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.contabilidad.asiento.Asiento;
import com.montanaritech.contable.contabilidad.asiento.AsientoLinea;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancaria;
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
 * Movimiento bancario importado (F5.1): entra siempre en {@code PENDIENTE} y
 * nunca impacta la contabilidad hasta que el usuario decide qué hacer con él
 * (ver {@link EstadoMovimientoBancario}). {@code importe}/{@code importeArs}
 * son con signo: positivo = ingreso (depósito), negativo = egreso (débito) —
 * convención estándar de extracto bancario, la misma que usarán los parsers
 * de F5.2.
 */
@Entity
@Table(name = "movimiento_bancario", uniqueConstraints = @UniqueConstraint(name = "uk_movimiento_bancario_asiento", columnNames = "asiento_id"))
@Getter
@Setter
public class MovimientoBancario extends EntidadNegocio {

    @ManyToOne(optional = false)
    @JoinColumn(name = "cuenta_bancaria_id", nullable = false, foreignKey = @ForeignKey(name = "fk_movimiento_bancario_cuenta_bancaria"))
    private CuentaBancaria cuentaBancaria;

    /** Nula cuando el origen de importación (F5.2) no trae fecha en la fila (ej. Galicia ARS): se completa con "corregir". */
    @Column
    private LocalDate fecha;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal importe;

    @ManyToOne(optional = false)
    @JoinColumn(name = "moneda_id", nullable = false, foreignKey = @ForeignKey(name = "fk_movimiento_bancario_moneda"))
    private Moneda moneda;

    @Column(name = "tipo_cambio", nullable = false, precision = 19, scale = 6)
    private BigDecimal tipoCambio;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuente_tc", length = 20)
    private AsientoLinea.FuenteTc fuenteTc;

    @Column(name = "importe_ars", nullable = false, precision = 18, scale = 2)
    private BigDecimal importeArs;

    @Column(length = 100)
    private String referencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen_importacion", nullable = false, length = 20)
    private OrigenImportacionMovimiento origenImportacion = OrigenImportacionMovimiento.MANUAL;

    /** Hash de fecha+importe+descripción+referencia (F5.2): único por cuenta bancaria, nulo en carga manual. Evita duplicar filas al re-importar el mismo resumen. */
    @Column(name = "hash_importacion", length = 64)
    private String hashImportacion;

    /** Cuenta propuesta al cargar el movimiento; "confirmar" la usa tal cual, "imputar" la ignora y pide otra. */
    @ManyToOne(optional = true)
    @JoinColumn(name = "cuenta_contable_sugerida_id", foreignKey = @ForeignKey(name = "fk_movimiento_bancario_cuenta_sugerida"))
    private CuentaContable cuentaContableSugerida;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoMovimientoBancario estado = EstadoMovimientoBancario.PENDIENTE;

    @ManyToOne(optional = true)
    @JoinColumn(name = "asiento_id", foreignKey = @ForeignKey(name = "fk_movimiento_bancario_asiento"))
    private Asiento asiento;

    @Column(name = "motivo_descarte", length = 500)
    private String motivoDescarte;

    @Column(length = 2000)
    private String observaciones;
}
