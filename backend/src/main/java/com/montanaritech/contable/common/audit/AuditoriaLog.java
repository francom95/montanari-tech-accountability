package com.montanaritech.contable.common.audit;

import com.montanaritech.contable.common.tenant.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Registro de auditoría (F1.1 §4): solo-inserción, nunca se edita ni se
 * borra ({@code @Immutable} le impide a Hibernate emitir UPDATE aunque
 * alguien modifique una instancia gestionada). Sin {@code tenant_id} no es
 * excepción (sí lo tiene), pero SÍ es excepción a llevar {@code version}
 * (no aplica: nunca hay una segunda escritura sobre la misma fila).
 *
 * <p>Esta clase es solo el modelo de datos. El servicio que la escribe
 * (invocado explícitamente desde cada operación sensible) se implementa en
 * F1.6, que también agrega la pantalla de consulta.
 */
@Entity
@Table(name = "auditoria_log")
@Getter
@Setter
@Immutable
public class AuditoriaLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "entidad_tipo", nullable = false, length = 60)
    private String entidadTipo;

    @Column(name = "entidad_id", nullable = false)
    private Long entidadId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccionAuditoria accion;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "fecha_hora", nullable = false)
    private Instant fechaHora;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_antes", columnDefinition = "json")
    private String datosAntes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_despues", columnDefinition = "json")
    private String datosDespues;

    @Column(name = "sobre_periodo_cerrado", nullable = false)
    private boolean sobrePeriodoCerrado;

    @Column(length = 500)
    private String detalle;

    @PrePersist
    protected void alPersistir() {
        if (tenantId == null) {
            tenantId = TenantContext.getTenantId();
        }
        if (fechaHora == null) {
            fechaHora = Instant.now();
        }
    }
}
