package com.montanaritech.contable.contabilidad.mayor.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mayor de una cuenta (F3.1 §5.2/§5.3), paginado para pantalla ({@code filas}
 * ya recortadas a la página pedida; el export usa la variante completa).
 *
 * <p>{@code vistaAnalitica}: cuando hay un filtro por rubro/proyecto/cliente/
 * proveedor/origen/moneda, el acumulado corre solo sobre ese subconjunto y
 * no es "el saldo contable de la cuenta" (F3.1 §5.4) — el frontend debe
 * rotularlo "saldo del filtro" en vez de "saldo de la cuenta".
 *
 * <p>{@code contrarioAlEsperado}: null cuando no aplica (vista analítica);
 * en caso contrario, {@code true} si el saldo final quedó del lado opuesto
 * a {@code saldoEsperado} de la cuenta — es solo informativo, nunca bloquea
 * nada (F3.1 §2.2/CP-20; la alerta real la emite el motor de F9.1, todavía
 * no implementado).
 */
public record MayorResponse(
        Long cuentaContableId,
        String cuentaContableCodigo,
        String cuentaContableNombre,
        boolean esCuentaMadre,
        boolean vistaAnalitica,
        List<MayorFilaResponse> filas,
        int page,
        int size,
        long totalFilas,
        int totalPaginas,
        BigDecimal saldoFinal,
        String saldoFinalEtiqueta,
        Boolean contrarioAlEsperado
) {}
