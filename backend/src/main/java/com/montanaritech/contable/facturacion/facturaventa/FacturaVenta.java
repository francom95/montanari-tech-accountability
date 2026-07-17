package com.montanaritech.contable.facturacion.facturaventa;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.contabilidad.asiento.Asiento;
import com.montanaritech.contable.contabilidad.asiento.AsientoLinea;
import com.montanaritech.contable.facturacion.TipoComprobante;
import com.montanaritech.contable.maestros.cliente.Cliente;
import com.montanaritech.contable.maestros.jurisdiccion.Jurisdiccion;
import com.montanaritech.contable.maestros.moneda.Moneda;
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
 * Factura de venta (F1.1 §6.5, F4.1/F4.2): cabecera + líneas. Genera un
 * asiento automático al confirmarse ({@link FacturaVentaAsientoGenerator});
 * el vínculo {@code asiento} es bidireccional (el asiento guarda
 * {@code origen = FACTURA_VENTA} + {@code origenTipo/origenId} apuntando acá).
 * {@code numero} es el número del comprobante en papel (string, p. ej.
 * "00001-00000123"), no el número interno del asiento.
 */
@Entity
@Table(name = "factura_venta", uniqueConstraints = @UniqueConstraint(
        name = "uk_factura_venta", columnNames = {"tenant_id", "cliente_id", "tipo_comprobante", "punto_venta", "numero"}))
@Getter
@Setter
public class FacturaVenta extends EntidadNegocio {

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, foreignKey = @ForeignKey(name = "fk_factura_venta_cliente"))
    private Cliente cliente;

    @ManyToOne(optional = true)
    @JoinColumn(name = "proyecto_id", foreignKey = @ForeignKey(name = "fk_factura_venta_proyecto"))
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

    @ManyToOne(optional = true)
    @JoinColumn(name = "jurisdiccion_destino_id", foreignKey = @ForeignKey(name = "fk_factura_venta_jurisdiccion"))
    private Jurisdiccion jurisdiccionDestino;

    @ManyToOne(optional = false)
    @JoinColumn(name = "moneda_id", nullable = false, foreignKey = @ForeignKey(name = "fk_factura_venta_moneda"))
    private Moneda moneda;

    @Column(name = "tipo_cambio", nullable = false, precision = 19, scale = 6)
    private BigDecimal tipoCambio;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuente_tc", length = 20)
    private AsientoLinea.FuenteTc fuenteTc;

    @Column(name = "neto_gravado", nullable = false, precision = 18, scale = 2)
    private BigDecimal netoGravado = BigDecimal.ZERO;

    @Column(name = "no_gravado", nullable = false, precision = 18, scale = 2)
    private BigDecimal noGravado = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal exento = BigDecimal.ZERO;

    @Column(name = "importe_iva", nullable = false, precision = 18, scale = 2)
    private BigDecimal importeIva = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal total;

    @Column(name = "total_ars", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalArs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoDocumento estado = EstadoDocumento.BORRADOR;

    @ManyToOne(optional = true)
    @JoinColumn(name = "asiento_id", foreignKey = @ForeignKey(name = "fk_factura_venta_asiento"))
    private Asiento asiento;

    @Column(length = 2000)
    private String observaciones;

    @OneToMany(mappedBy = "facturaVenta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<FacturaVentaLinea> lineas = new ArrayList<>();
}
