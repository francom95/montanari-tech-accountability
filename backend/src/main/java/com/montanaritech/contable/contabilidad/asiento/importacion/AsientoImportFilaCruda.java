package com.montanaritech.contable.contabilidad.asiento.importacion;

/**
 * Fila cruda del CSV pre-decodificado del Libro Diario (F10.2), columnas
 * fijas en orden: {@code numeroAsientoOriginal} (agrupador — todas las
 * líneas con el mismo número forman un {@code Asiento}), {@code fecha},
 * {@code codigoDecodificado} (código de cuenta ya decodificado y traducido,
 * ver {@link AsientoImportParser}), {@code debe}, {@code haber}, {@code leyenda}.
 */
public record AsientoImportFilaCruda(
        int numeroFila,
        String numeroAsientoOriginal,
        String fecha,
        String codigoDecodificado,
        String debe,
        String haber,
        String leyenda
) {}
