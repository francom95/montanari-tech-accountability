package com.montanaritech.contable.common.secuencia;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Contador correlativo por tenant (F3.1 §3.2). Una fila por nombre de
 * secuencia (p. ej. {@code ASIENTO}); se lee y actualiza con lock pesimista
 * ({@link SecuenciaRepository#buscarParaActualizar}) dentro de la
 * transacción del confirmar, para que dos confirmaciones concurrentes no
 * puedan obtener el mismo número.
 */
@Entity
@Table(name = "secuencia", uniqueConstraints = @UniqueConstraint(name = "uk_secuencia_tenant_nombre", columnNames = {"tenant_id", "nombre"}))
@Getter
@Setter
public class Secuencia extends EntidadNegocio {

    @Column(nullable = false, length = 40)
    private String nombre;

    @Column(name = "valor_actual", nullable = false)
    private long valorActual;
}
