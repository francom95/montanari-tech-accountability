package com.montanaritech.contable.pendiente;

import com.montanaritech.contable.auth.Usuario;
import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.maestros.cliente.Cliente;
import com.montanaritech.contable.maestros.proveedor.Proveedor;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * Pendiente administrativo (F8.5, molde PL-1): recordatorios, controles
 * manuales, revisiones del contador, ajustes pendientes, facturas a pedir,
 * pagos a verificar, movimientos bancarios a identificar, impuestos a
 * revisar. La fecha de creación ya la aporta {@code creadoEn} (auditoría de
 * {@link EntidadNegocio}), no se duplica como campo propio.
 * {@code fechaEstimadaResolucion} alimenta la query de alertas (F9.1) vía
 * {@code proximosAVencer}.
 */
@Entity
@Table(name = "pendiente_administrativo")
@Getter
@Setter
public class PendienteAdministrativo extends EntidadNegocio {

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 2000)
    private String descripcion;

    @Column(name = "fecha_estimada_resolucion")
    private LocalDate fechaEstimadaResolucion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PrioridadPendiente prioridad = PrioridadPendiente.MEDIA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPendiente estado = EstadoPendiente.PENDIENTE;

    @ManyToOne(optional = true)
    @JoinColumn(name = "responsable_id", foreignKey = @ForeignKey(name = "fk_pendiente_responsable"))
    private Usuario responsable;

    @Column(length = 100)
    private String categoria;

    @ManyToOne(optional = true)
    @JoinColumn(name = "proyecto_id", foreignKey = @ForeignKey(name = "fk_pendiente_proyecto"))
    private Proyecto proyecto;

    @ManyToOne(optional = true)
    @JoinColumn(name = "cliente_id", foreignKey = @ForeignKey(name = "fk_pendiente_cliente"))
    private Cliente cliente;

    @ManyToOne(optional = true)
    @JoinColumn(name = "proveedor_id", foreignKey = @ForeignKey(name = "fk_pendiente_proveedor"))
    private Proveedor proveedor;

    @Column(length = 2000)
    private String observaciones;

    @Column(nullable = false)
    private boolean activo = true;
}
