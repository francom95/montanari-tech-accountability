package com.montanaritech.contable.alerta;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * Alerta generada por {@code MotorAlertasService} (F9.1). La unicidad por
 * {@code (tenant, tipo, entidadTipo, entidadRefId)} es lo que permite
 * sincronizar sin duplicar: para una misma condición vigente siempre existe
 * a lo sumo una fila, que se reactiva/actualiza en vez de crear otra.
 *
 * <p>{@code estado} implementa la decisión de auto-resolución (F9.1): el
 * motor marca RESUELTA una alerta ACTIVA en cuanto su condición deja de
 * cumplirse, sin esperar a que nadie la lea — leída (F9.1, por usuario) y
 * resuelta son ejes independientes.
 */
@Entity
@Table(name = "alerta", uniqueConstraints = @UniqueConstraint(name = "uk_alerta_tenant_tipo_entidad",
        columnNames = {"tenant_id", "tipo", "entidad_tipo", "entidad_ref_id"}))
@Getter
@Setter
public class Alerta extends EntidadNegocio {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoAlerta tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeveridadAlerta severidad;

    @Column(nullable = false, length = 300)
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @Column(name = "entidad_tipo", nullable = false, length = 30)
    private TipoEntidadAlerta entidadTipo;

    @Column(name = "entidad_ref_id", nullable = false)
    private Long entidadRefId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAlerta estado = EstadoAlerta.ACTIVA;

    @Column(name = "resuelta_en")
    private Instant resueltaEn;
}
