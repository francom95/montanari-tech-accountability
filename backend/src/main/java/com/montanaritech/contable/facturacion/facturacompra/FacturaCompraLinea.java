package com.montanaritech.contable.facturacion.facturacompra;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.maestros.tipocosto.TipoCosto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * Línea de {@link FacturaCompra} (F1.1 §6.5). {@code cuentaContable} es
 * opcional: si no se fija, el generador la resuelve vía {@code mapeo_cuenta}
 * (concepto {@code COSTO_GASTO}, discriminada por {@code tipoCosto}).
 */
@Entity
@Table(name = "factura_compra_linea")
@Getter
@Setter
public class FacturaCompraLinea extends EntidadNegocio {

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "factura_compra_id", nullable = false, foreignKey = @ForeignKey(name = "fk_factura_compra_linea_factura"))
    private FacturaCompra facturaCompra;

    @Column(nullable = false)
    private Integer orden;

    @Column(nullable = false, length = 500)
    private String descripcion;

    /** Discriminador de {@code COSTO_GASTO} en {@code mapeo_cuenta} cuando {@code cuentaContable} no se fija (F4.1 §1.3). */
    @ManyToOne(optional = false)
    @JoinColumn(name = "tipo_costo_id", nullable = false, foreignKey = @ForeignKey(name = "fk_factura_compra_linea_tipo_costo"))
    private TipoCosto tipoCosto;

    @Column(name = "importe_neto", nullable = false, precision = 18, scale = 2)
    private BigDecimal importeNeto;

    @Column(name = "alicuota_iva", nullable = false, precision = 5, scale = 2)
    private BigDecimal alicuotaIva;

    @Column(name = "importe_iva", nullable = false, precision = 18, scale = 2)
    private BigDecimal importeIva;

    @ManyToOne(optional = true)
    @JoinColumn(name = "cuenta_contable_id", foreignKey = @ForeignKey(name = "fk_factura_compra_linea_cuenta"))
    private CuentaContable cuentaContable;
}
