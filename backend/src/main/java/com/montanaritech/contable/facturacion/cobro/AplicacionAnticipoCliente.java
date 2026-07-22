package com.montanaritech.contable.facturacion.cobro;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.contabilidad.asiento.Asiento;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVenta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * Aplicación posterior de un anticipo de cliente contra una factura de venta
 * (F4.1 §6.5, CO-5): registro append-only, cada fila genera su propio
 * asiento {@code AJUSTE} — el asiento del cobro original que generó el
 * anticipo jamás se edita (F3.1 §6.5, D-4).
 */
@Entity
@Table(name = "aplicacion_anticipo_cliente")
@Getter
@Setter
public class AplicacionAnticipoCliente extends EntidadNegocio {

    @ManyToOne(optional = false)
    @JoinColumn(name = "cobro_id", nullable = false, foreignKey = @ForeignKey(name = "fk_aplicacion_anticipo_cliente_cobro"))
    private Cobro cobro;

    @ManyToOne(optional = false)
    @JoinColumn(name = "factura_venta_id", nullable = false, foreignKey = @ForeignKey(name = "fk_aplicacion_anticipo_cliente_factura"))
    private FacturaVenta facturaVenta;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "monto_original", nullable = false, precision = 18, scale = 2)
    private BigDecimal montoOriginal;

    @Column(name = "monto_ars_cancelado", nullable = false, precision = 18, scale = 2)
    private BigDecimal montoArsCancelado;

    @ManyToOne(optional = false)
    @JoinColumn(name = "asiento_id", nullable = false, foreignKey = @ForeignKey(name = "fk_aplicacion_anticipo_cliente_asiento"))
    private Asiento asiento;
}
