package com.montanaritech.contable.maestros.proyecto.etapa;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.maestros.proveedor.Proveedor;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 * Molde F1.8 PL-1 (F2.5). Etapa de un {@link Proyecto}: varias pueden estar
 * {@code EN_CURSO} simultáneamente (sin exclusión de estados entre etapas
 * del mismo proyecto). Proveedores N:M — mismo patrón que
 * {@code proveedor_tipo_costo} (F2.3).
 */
@Entity
@Table(name = "etapa")
@Getter
@Setter
public class Etapa extends EntidadNegocio {

    @ManyToOne(optional = false)
    @JoinColumn(name = "proyecto_id", nullable = false, foreignKey = @ForeignKey(name = "fk_etapa_proyecto"))
    private Proyecto proyecto;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(length = 2000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEtapa estado = EstadoEtapa.PENDIENTE;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_estimada_fin")
    private LocalDate fechaEstimadaFin;

    @Column(name = "porcentaje_avance")
    private Integer porcentajeAvance;

    @Column(name = "monto_presupuestado", precision = 18, scale = 2)
    private BigDecimal montoPresupuestado;

    @Column(name = "costos_estimados", precision = 18, scale = 2)
    private BigDecimal costosEstimados;

    @ManyToMany
    @JoinTable(
            name = "etapa_proveedor",
            joinColumns = @JoinColumn(name = "etapa_id"),
            inverseJoinColumns = @JoinColumn(name = "proveedor_id")
    )
    private Set<Proveedor> proveedores = new HashSet<>();

    @Column(name = "pagos_previstos", precision = 18, scale = 2)
    private BigDecimal pagosPrevistos;

    @Column(name = "cobros_previstos", precision = 18, scale = 2)
    private BigDecimal cobrosPrevistos;

    @Column(length = 2000)
    private String observaciones;

    @Column(nullable = false)
    private boolean activo = true;

    public enum EstadoEtapa {
        PENDIENTE,
        EN_CURSO,
        FINALIZADA,
        CANCELADA
    }
}
