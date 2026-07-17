package com.montanaritech.contable.contabilidad.asiento;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.maestros.cliente.Cliente;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancaria;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.proveedor.Proveedor;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.proyecto.etapa.Etapa;
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
 * Línea debe/haber de un {@link Asiento} (F3.1 §3.1/§3.3). Exactamente uno
 * de {@code debe}/{@code haber} es distinto de cero al confirmar
 * ({@code AsientoService} valida {@code LINEA_DEBE_XOR_HABER}; no hay CHECK
 * de fila porque un borrador puede tener una línea en blanco todavía sin
 * completar). {@code debe}/{@code haber} son siempre el importe ya
 * convertido a ARS; {@code moneda}/{@code tipoCambio}/{@code importeOriginal}
 * preservan la moneda de origen de la operación (F3.1 §3.3/§6.1). Las
 * dimensiones analíticas (proyecto, etapa, cliente, proveedor, cuenta
 * bancaria como destino de fondos) son opcionales y van por línea (F3.1
 * §3.1, decisión D-1). {@code generadaAuto} distingue, dentro de un asiento
 * automático (F4.x), las líneas del generador de las agregadas a mano; F3.4
 * (carga manual) siempre la deja en {@code false}.
 */
@Entity
@Table(name = "asiento_linea")
@Getter
@Setter
public class AsientoLinea extends EntidadNegocio {

    /**
     * {@code @JsonIgnore}: {@code @Auditado}/{@code AuditoriaService} serializan
     * la entidad JPA cruda; sin este corte, Asiento.lineas -> AsientoLinea.asiento
     * -> Asiento.lineas... recursa infinito (mismo criterio que
     * {@code ProyectoCuota.proyecto}, F2.5).
     */
    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "asiento_id", nullable = false, foreignKey = @ForeignKey(name = "fk_asiento_linea_asiento"))
    private Asiento asiento;

    @Column(nullable = false)
    private Integer orden;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cuenta_contable_id", nullable = false, foreignKey = @ForeignKey(name = "fk_asiento_linea_cuenta"))
    private CuentaContable cuentaContable;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal debe = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal haber = BigDecimal.ZERO;

    @ManyToOne(optional = false)
    @JoinColumn(name = "moneda_id", nullable = false, foreignKey = @ForeignKey(name = "fk_asiento_linea_moneda"))
    private Moneda moneda;

    @Column(name = "tipo_cambio", precision = 19, scale = 6)
    private BigDecimal tipoCambio;

    @Column(name = "importe_original", precision = 18, scale = 2)
    private BigDecimal importeOriginal;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuente_tc", length = 20)
    private FuenteTc fuenteTc;

    @Column(length = 500)
    private String leyenda;

    @ManyToOne(optional = true)
    @JoinColumn(name = "proyecto_id", foreignKey = @ForeignKey(name = "fk_asiento_linea_proyecto"))
    private Proyecto proyecto;

    @ManyToOne(optional = true)
    @JoinColumn(name = "etapa_id", foreignKey = @ForeignKey(name = "fk_asiento_linea_etapa"))
    private Etapa etapa;

    @ManyToOne(optional = true)
    @JoinColumn(name = "cliente_id", foreignKey = @ForeignKey(name = "fk_asiento_linea_cliente"))
    private Cliente cliente;

    @ManyToOne(optional = true)
    @JoinColumn(name = "proveedor_id", foreignKey = @ForeignKey(name = "fk_asiento_linea_proveedor"))
    private Proveedor proveedor;

    /** Destino/origen de fondos de la línea (F3.1 §3.1: {@code cuenta_financiera_id}). */
    @ManyToOne(optional = true)
    @JoinColumn(name = "cuenta_bancaria_id", foreignKey = @ForeignKey(name = "fk_asiento_linea_cuenta_bancaria"))
    private CuentaBancaria cuentaBancaria;

    @Column(name = "generada_auto", nullable = false)
    private boolean generadaAuto = false;

    public enum FuenteTc {
        MANUAL,
        AUTOMATICO
    }
}
