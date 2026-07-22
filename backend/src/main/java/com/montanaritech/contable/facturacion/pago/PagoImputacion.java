package com.montanaritech.contable.facturacion.pago;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.facturacion.facturacompra.FacturaCompra;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/** Línea de {@link Pago}: cuánto se imputa contra una factura de compra puntual. */
@Entity
@Table(name = "pago_imputacion")
@Getter
@Setter
public class PagoImputacion extends EntidadNegocio {

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "pago_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pago_imputacion_pago"))
    private Pago pago;

    @ManyToOne(optional = false)
    @JoinColumn(name = "factura_compra_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pago_imputacion_factura"))
    private FacturaCompra facturaCompra;

    @Column(nullable = false)
    private Integer orden;

    @Column(name = "monto_imputado_original", nullable = false, precision = 18, scale = 2)
    private BigDecimal montoImputadoOriginal;

    @Column(name = "monto_ars_cancelado", precision = 18, scale = 2)
    private BigDecimal montoArsCancelado;
}
