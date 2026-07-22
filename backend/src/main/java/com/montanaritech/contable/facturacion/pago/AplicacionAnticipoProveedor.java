package com.montanaritech.contable.facturacion.pago;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.contabilidad.asiento.Asiento;
import com.montanaritech.contable.facturacion.facturacompra.FacturaCompra;
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

/** Aplicación posterior de un anticipo a proveedor contra una factura de compra (F4.1 §6.5), simétrico a {@code AplicacionAnticipoCliente}. */
@Entity
@Table(name = "aplicacion_anticipo_proveedor")
@Getter
@Setter
public class AplicacionAnticipoProveedor extends EntidadNegocio {

    @ManyToOne(optional = false)
    @JoinColumn(name = "pago_id", nullable = false, foreignKey = @ForeignKey(name = "fk_aplicacion_anticipo_proveedor_pago"))
    private Pago pago;

    @ManyToOne(optional = false)
    @JoinColumn(name = "factura_compra_id", nullable = false, foreignKey = @ForeignKey(name = "fk_aplicacion_anticipo_proveedor_factura"))
    private FacturaCompra facturaCompra;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "monto_original", nullable = false, precision = 18, scale = 2)
    private BigDecimal montoOriginal;

    @Column(name = "monto_ars_cancelado", nullable = false, precision = 18, scale = 2)
    private BigDecimal montoArsCancelado;

    @ManyToOne(optional = false)
    @JoinColumn(name = "asiento_id", nullable = false, foreignKey = @ForeignKey(name = "fk_aplicacion_anticipo_proveedor_asiento"))
    private Asiento asiento;
}
