package com.montanaritech.contable.maestros.proyecto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.montanaritech.contable.common.tenant.EntidadNegocio;
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

/** Cuota pactada de cobro de un {@link Proyecto} (F2.5). */
@Entity
@Table(name = "proyecto_cuota")
@Getter
@Setter
public class ProyectoCuota extends EntidadNegocio {

    /**
     * {@code @JsonIgnore}: {@code @Auditado} serializa la entidad JPA cruda
     * (no el DTO) para el log de auditoría; sin este corte, Proyecto.cuotas
     * -> ProyectoCuota.proyecto -> Proyecto.cuotas... recursa infinito.
     */
    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "proyecto_id", nullable = false, foreignKey = @ForeignKey(name = "fk_proyecto_cuota_proyecto"))
    private Proyecto proyecto;

    @Column(nullable = false)
    private Integer numero;

    @Column(name = "fecha_estimada_cobro", nullable = false)
    private LocalDate fechaEstimadaCobro;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal importe;
}
