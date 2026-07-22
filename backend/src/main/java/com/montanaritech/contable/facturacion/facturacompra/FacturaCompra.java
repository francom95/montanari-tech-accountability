package com.montanaritech.contable.facturacion.facturacompra;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.contabilidad.asiento.Asiento;
import com.montanaritech.contable.contabilidad.asiento.AsientoLinea;
import com.montanaritech.contable.facturacion.TipoComprobante;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.proveedor.Proveedor;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
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
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Factura de compra (F4.1 §5, F4.3): simétrica a {@link
 * com.montanaritech.contable.facturacion.facturaventa.FacturaVenta}, mismo
 * modelo para notas de crédito/débito (ADR-13, signo derivado de {@code
 * tipoComprobante}). Las percepciones sufridas se guardan aparte en {@code
 * comprobante_tributo} (vínculo por {@code comprobanteTipo}/{@code
 * comprobanteId}, no una relación JPA).
 */
@Entity
@Table(name = "factura_compra", uniqueConstraints = @UniqueConstraint(
        name = "uk_factura_compra", columnNames = {"tenant_id", "proveedor_id", "tipo_comprobante", "punto_venta", "numero"}))
@Getter
@Setter
public class FacturaCompra extends EntidadNegocio {

    @ManyToOne(optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false, foreignKey = @ForeignKey(name = "fk_factura_compra_proveedor"))
    private Proveedor proveedor;

    @ManyToOne(optional = true)
    @JoinColumn(name = "proyecto_id", foreignKey = @ForeignKey(name = "fk_factura_compra_proyecto"))
    private Proyecto proyecto;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comprobante", nullable = false, length = 20)
    private TipoComprobante tipoComprobante;

    @Column(name = "punto_venta", length = 20)
    private String puntoVenta;

    @Column(nullable = false, length = 20)
    private String numero;

    @ManyToOne(optional = false)
    @JoinColumn(name = "moneda_id", nullable = false, foreignKey = @ForeignKey(name = "fk_factura_compra_moneda"))
    private Moneda moneda;

    @Column(name = "tipo_cambio", nullable = false, precision = 19, scale = 6)
    private BigDecimal tipoCambio;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuente_tc", length = 20)
    private AsientoLinea.FuenteTc fuenteTc;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal neto = BigDecimal.ZERO;

    @Column(name = "importe_iva", nullable = false, precision = 18, scale = 2)
    private BigDecimal importeIva = BigDecimal.ZERO;

    @Column(name = "importe_percepciones", nullable = false, precision = 18, scale = 2)
    private BigDecimal importePercepciones = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal total;

    @Column(name = "total_ars", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalArs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoDocumento estado = EstadoDocumento.BORRADOR;

    @ManyToOne(optional = true)
    @JoinColumn(name = "asiento_id", foreignKey = @ForeignKey(name = "fk_factura_compra_asiento"))
    private Asiento asiento;

    @Column(length = 2000)
    private String observaciones;

    @OneToMany(mappedBy = "facturaCompra", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<FacturaCompraLinea> lineas = new ArrayList<>();
}
