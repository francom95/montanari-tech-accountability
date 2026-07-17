package com.montanaritech.contable.contabilidad.asiento.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Línea de {@link AsientoEditarConfirmadoRequest}. A diferencia de
 * {@link AsientoLineaRequest} (crear/editar borrador, siempre reemplazo
 * total), acá {@code id} identifica una línea existente: {@code null}
 * significa línea manual nueva. El service usa {@code id} para distinguir,
 * de las líneas {@code generada_auto = true} ya persistidas, cuáles el
 * pedido deja intactas (cualquier usuario) de cuáles modifica o quita
 * (F3.1 §4.2: solo ADMIN).
 */
public record AsientoLineaEditarRequest(
        Long id,
        @NotNull Long cuentaContableId,
        @NotNull @DecimalMin(value = "0.00") BigDecimal debe,
        @NotNull @DecimalMin(value = "0.00") BigDecimal haber,
        @NotNull Long monedaId,
        BigDecimal tipoCambio,
        BigDecimal importeOriginal,
        String leyenda,
        Long proyectoId,
        Long etapaId,
        Long clienteId,
        Long proveedorId,
        Long cuentaBancariaId
) {}
