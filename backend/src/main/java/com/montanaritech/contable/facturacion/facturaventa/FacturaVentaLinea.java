package com.montanaritech.contable.facturacion.facturaventa;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
 * Línea de {@link FacturaVenta} (F1.1 §6.5). {@code cuentaContable} es
 * opcional: si no se fija, el generador la resuelve vía {@code mapeo_cuenta}
 * (concepto {@code INGRESO_VENTA}, discriminada por {@code tipoIngreso}).
 */
@Entity
@Table(name = "factura_venta_linea")
@Getter
@Setter
public class FacturaVentaLinea extends EntidadNegocio {

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "factura_venta_id", nullable = false, foreignKey = @ForeignKey(name = "fk_factura_venta_linea_factura"))
    private FacturaVenta facturaVenta;

    @Column(nullable = false)
    private Integer orden;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoLineaFactura tipo = TipoLineaFactura.GRAVADO;

    /** Discriminador de {@code INGRESO_VENTA} en {@code mapeo_cuenta} cuando {@code cuentaContable} no se fija (F4.1 §1.3). */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_ingreso", nullable = false, length = 20)
    private TipoIngreso tipoIngreso = TipoIngreso.VENTA;

    @Column(name = "importe_neto", nullable = false, precision = 18, scale = 2)
    private BigDecimal importeNeto;

    @Column(name = "alicuota_iva", nullable = false, precision = 5, scale = 2)
    private BigDecimal alicuotaIva;

    @Column(name = "importe_iva", nullable = false, precision = 18, scale = 2)
    private BigDecimal importeIva;

    @ManyToOne(optional = true)
    @JoinColumn(name = "cuenta_contable_id", foreignKey = @ForeignKey(name = "fk_factura_venta_linea_cuenta"))
    private CuentaContable cuentaContable;
}
