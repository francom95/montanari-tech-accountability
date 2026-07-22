package com.montanaritech.contable.facturacion.cobro;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.contabilidad.asiento.Asiento;
import com.montanaritech.contable.contabilidad.asiento.AsientoLinea;
import com.montanaritech.contable.maestros.cliente.Cliente;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancaria;
import com.montanaritech.contable.maestros.moneda.Moneda;
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
 * Cobro (F4.1 §6.1, F4.4): imputa {@code total} contra 0..N facturas de
 * venta. El remanente no imputado ({@code total - Σ imputaciones}) queda
 * como anticipo del cliente ({@code montoAnticipo}, congelado al confirmar) —
 * un cobro sin líneas de imputación es 100% anticipo, sin caso especial en
 * el generador. Las retenciones sufridas viven en {@code comprobante_tributo}
 * ({@code ComprobanteTipo.COBRO}), no como relación JPA acá.
 */
@Entity
@Table(name = "cobro")
@Getter
@Setter
public class Cobro extends EntidadNegocio {

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cobro_cliente"))
    private Cliente cliente;

    @Column(nullable = false)
    private LocalDate fecha;

    @ManyToOne(optional = false)
    @JoinColumn(name = "moneda_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cobro_moneda"))
    private Moneda moneda;

    @Column(name = "tipo_cambio", nullable = false, precision = 19, scale = 6)
    private BigDecimal tipoCambio;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuente_tc", length = 20)
    private AsientoLinea.FuenteTc fuenteTc;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cuenta_bancaria_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cobro_cuenta_bancaria"))
    private CuentaBancaria cuentaBancaria;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal total;

    @Column(name = "total_ars", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalArs;

    @Column(name = "importe_retenciones", nullable = false, precision = 18, scale = 2)
    private BigDecimal importeRetenciones = BigDecimal.ZERO;

    /** Remanente no imputado a facturas (Debe Fondos / Haber ANTICIPO_CLIENTE), congelado al confirmar. */
    @Column(name = "monto_anticipo", nullable = false, precision = 18, scale = 2)
    private BigDecimal montoAnticipo = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoDocumento estado = EstadoDocumento.BORRADOR;

    @ManyToOne(optional = true)
    @JoinColumn(name = "asiento_id", foreignKey = @ForeignKey(name = "fk_cobro_asiento"))
    private Asiento asiento;

    @Column(length = 2000)
    private String observaciones;

    @OneToMany(mappedBy = "cobro", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<CobroImputacion> lineas = new ArrayList<>();
}
