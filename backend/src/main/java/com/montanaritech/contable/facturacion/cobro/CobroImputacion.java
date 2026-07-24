package com.montanaritech.contable.facturacion.cobro;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVenta;
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
 * Línea de {@link Cobro}: cuánto de este cobro se imputa contra una factura
 * de venta puntual. {@code montoArsCancelado} queda {@code null} mientras el
 * cobro es BORRADOR — el generador lo calcula y lo fija (regla del residuo,
 * F3.1 §6.3) recién al confirmar.
 */
@Entity
@Table(name = "cobro_imputacion")
@Getter
@Setter
public class CobroImputacion extends EntidadNegocio {

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "cobro_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cobro_imputacion_cobro"))
    private Cobro cobro;

    @ManyToOne(optional = false)
    @JoinColumn(name = "factura_venta_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cobro_imputacion_factura"))
    private FacturaVenta facturaVenta;

    @Column(nullable = false)
    private Integer orden;

    @Column(name = "monto_imputado_original", nullable = false, precision = 18, scale = 2)
    private BigDecimal montoImputadoOriginal;

    @Column(name = "monto_ars_cancelado", precision = 18, scale = 2)
    private BigDecimal montoArsCancelado;

    /**
     * Recargo por mora (F7.4), en la misma moneda que {@code montoImputadoOriginal}.
     * {@code null} o cero: cobro sin recargo, comportamiento idéntico al de
     * antes de F7.4. Nunca pasa por {@code CalculoImputacion} — no cancela
     * deuda de la factura, es ingreso nuevo.
     */
    @Column(name = "recargo_mora_original", precision = 18, scale = 2)
    private BigDecimal recargoMoraOriginal;
}
