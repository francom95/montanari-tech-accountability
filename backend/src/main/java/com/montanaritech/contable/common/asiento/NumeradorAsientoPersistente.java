package com.montanaritech.contable.common.asiento;

import com.montanaritech.contable.common.secuencia.Secuencia;
import com.montanaritech.contable.common.secuencia.SecuenciaRepository;
import com.montanaritech.contable.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación real de {@link NumeradorAsiento} (F3.4), respaldada en la
 * tabla {@code secuencia} (fila {@code ASIENTO}) con lock pesimista — sin
 * huecos, sin reuso, segura entre instancias. Reemplaza al placeholder
 * {@code NumeradorAsientoEnMemoria} que anunciaba este reemplazo.
 *
 * <p>{@code REQUIRED} (no {@code REQUIRES_NEW}): debe ejecutar dentro de la
 * misma transacción que confirma el asiento, para que el número asignado
 * quede atado al commit del asiento (si la confirmación falla después,
 * el rollback también revierte el incremento de la secuencia).
 */
@Component
@RequiredArgsConstructor
public class NumeradorAsientoPersistente implements NumeradorAsiento {

    private static final String SECUENCIA_ASIENTO = "ASIENTO";

    private final SecuenciaRepository repo;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public Long siguienteNumero() {
        Long tenantId = TenantContext.getTenantId();
        Secuencia secuencia = repo.buscarParaActualizar(tenantId, SECUENCIA_ASIENTO)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe la secuencia 'ASIENTO' para el tenant " + tenantId + " (falta el seed de V18)"));
        secuencia.setValorActual(secuencia.getValorActual() + 1);
        return secuencia.getValorActual();
    }
}
