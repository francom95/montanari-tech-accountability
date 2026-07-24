package com.montanaritech.contable.maestros.proyecto;

import com.montanaritech.contable.auth.Usuario;
import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.maestros.cliente.Cliente;
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
 * Molde F1.8 PL-1 (F2.5). Proyecto: cliente y responsable (FK), moneda +
 * monto total, cuotas pactadas (1:N, {@link ProyectoCuota}), estados
 * comercial/facturación/cobranza independientes entre sí. Las etapas
 * ({@link com.montanaritech.contable.maestros.proyecto.etapa.Etapa}) no se
 * modelan como colección acá: tienen su propio ciclo de vida (CRUD +
 * importación) y se consultan por proyecto_id.
 */
@Entity
@Table(name = "proyecto")
@Getter
@Setter
public class Proyecto extends EntidadNegocio {

    @Column(nullable = false, length = 160)
    private String nombre;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, foreignKey = @ForeignKey(name = "fk_proyecto_cliente"))
    private Cliente cliente;

    @ManyToOne(optional = true)
    @JoinColumn(name = "responsable_id", foreignKey = @ForeignKey(name = "fk_proyecto_responsable"))
    private Usuario responsable;

    @Column(length = 80)
    private String pais;

    /**
     * Distingue qué cascada de impuestos/comisiones aplica el presupuesto
     * (F2.6): Argentina (IVA + IIBB/Convenio Multilateral doméstico) vs
     * Exterior (comisiones bancarias COMEX + percepciones sobre la
     * transferencia, sin IVA/IIBB directo — la venta de servicios al
     * exterior está exenta). Reutiliza la columna existente (antes texto
     * libre sin uso real todavía).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_proyecto", length = 20)
    private TipoProyecto tipoProyecto = TipoProyecto.ARGENTINA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoProyecto estado = EstadoProyecto.PROSPECTO;

    @ManyToOne(optional = false)
    @JoinColumn(name = "moneda_id", nullable = false, foreignKey = @ForeignKey(name = "fk_proyecto_moneda"))
    private Moneda moneda;

    @Column(name = "monto_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "cantidad_pagos_pactados")
    private Integer cantidadPagosPactados;

    @Column(length = 2000)
    private String comentarios;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_comercial", nullable = false, length = 20)
    private EstadoComercial estadoComercial = EstadoComercial.PROSPECTO;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_facturacion", nullable = false, length = 20)
    private EstadoFacturacion estadoFacturacion = EstadoFacturacion.NO_FACTURADO;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_cobranza", nullable = false, length = 20)
    private EstadoCobranza estadoCobranza = EstadoCobranza.PENDIENTE;

    @Column(name = "fecha_estimada_finalizacion")
    private LocalDate fechaEstimadaFinalizacion;

    @Column(name = "fecha_real_finalizacion")
    private LocalDate fechaRealFinalizacion;

    @Column(nullable = false)
    private boolean activo = true;

    @OneToMany(mappedBy = "proyecto", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numero ASC")
    private List<ProyectoCuota> cuotas = new ArrayList<>();

    public enum EstadoProyecto {
        PROSPECTO,
        EN_CURSO,
        PAUSADO,
        FINALIZADO,
        CANCELADO
    }

    public enum EstadoComercial {
        PROSPECTO,
        EN_NEGOCIACION,
        GANADO,
        PERDIDO
    }

    public enum EstadoFacturacion {
        NO_FACTURADO,
        PARCIALMENTE_FACTURADO,
        FACTURADO_TOTAL
    }

    public enum EstadoCobranza {
        PENDIENTE,
        PARCIAL,
        COBRADO_TOTAL
    }

    public enum TipoProyecto {
        ARGENTINA,
        EXTERIOR
    }
}
