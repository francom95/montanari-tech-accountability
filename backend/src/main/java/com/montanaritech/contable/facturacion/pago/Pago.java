package com.montanaritech.contable.facturacion.pago;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.contabilidad.asiento.Asiento;
import com.montanaritech.contable.contabilidad.asiento.AsientoLinea;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancaria;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.proveedor.Proveedor;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Pago (F4.1 §6.2/§6.3/§6.5, F4.4): simétrico a {@link
 * com.montanaritech.contable.facturacion.cobro.Cobro}, sin retenciones
 * (Montanari no es agente de retención, checkpoint F4.1 #3).
 */
@Entity
@Table(name = "pago")
@Getter
@Setter
public class Pago extends EntidadNegocio {

    @ManyToOne(optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pago_proveedor"))
    private Proveedor proveedor;

    @Column(nullable = false)
    private LocalDate fecha;

    @ManyToOne(optional = false)
    @JoinColumn(name = "moneda_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pago_moneda"))
    private Moneda moneda;

    @Column(name = "tipo_cambio", nullable = false, precision = 19, scale = 6)
    private BigDecimal tipoCambio;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuente_tc", length = 20)
    private AsientoLinea.FuenteTc fuenteTc;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cuenta_bancaria_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pago_cuenta_bancaria"))
    private CuentaBancaria cuentaBancaria;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal total;

    @Column(name = "total_ars", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalArs;

    /** Remanente no imputado (Debe ANTICIPO_PROVEEDOR / Haber Fondos), congelado al confirmar. */
    @Column(name = "monto_anticipo", nullable = false, precision = 18, scale = 2)
    private BigDecimal montoAnticipo = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoDocumento estado = EstadoDocumento.BORRADOR;

    @ManyToOne(optional = true)
    @JoinColumn(name = "asiento_id", foreignKey = @ForeignKey(name = "fk_pago_asiento"))
    private Asiento asiento;

    @Column(length = 2000)
    private String observaciones;

    @OneToMany(mappedBy = "pago", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<PagoImputacion> lineas = new ArrayList<>();
}
