package com.montanaritech.contable.common.estado;

import com.montanaritech.contable.common.error.NegocioException;
import java.util.Map;
import java.util.Set;

/**
 * Transiciones válidas: BORRADOR→CONFIRMADO, CONFIRMADO→ANULADO. ANULADO es
 * terminal (no hay transición que salga de ahí). El service de cada
 * entidad llama a {@link #validar} antes de aplicar el cambio y audita el
 * resultado con {@code AuditoriaService.registrar(CAMBIO_ESTADO, ...)}
 * (ver {@code MonedaService} para el patrón de auditoría explícita).
 */
public final class TransicionEstadoValidator {

    private static final Map<EstadoDocumento, Set<EstadoDocumento>> TRANSICIONES_VALIDAS = Map.of(
            EstadoDocumento.BORRADOR, Set.of(EstadoDocumento.CONFIRMADO),
            EstadoDocumento.CONFIRMADO, Set.of(EstadoDocumento.ANULADO),
            EstadoDocumento.ANULADO, Set.of());

    private TransicionEstadoValidator() {
    }

    public static void validar(EstadoDocumento actual, EstadoDocumento nuevo) {
        if (!TRANSICIONES_VALIDAS.get(actual).contains(nuevo)) {
            throw new NegocioException(
                    "TRANSICION_ESTADO_INVALIDA",
                    "No se puede pasar de %s a %s".formatted(actual, nuevo));
        }
    }
}
