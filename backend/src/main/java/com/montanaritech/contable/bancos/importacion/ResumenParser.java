package com.montanaritech.contable.bancos.importacion;

import com.montanaritech.contable.bancos.movimientobancario.OrigenImportacionMovimiento;
import java.util.List;

/**
 * Estrategia de parsing de un resumen bancario/tarjeta (F5.2): un
 * {@code ResumenParser} por origen/formato, para poder sumar bancos nuevos
 * sin tocar el core de importación. Nunca persiste nada — solo normaliza el
 * archivo a filas {@link MovimientoParseado}.
 */
public interface ResumenParser {

    OrigenImportacionMovimiento origen();

    /**
     * @throws com.montanaritech.contable.common.error.NegocioException si el archivo no tiene el formato esperado
     */
    List<MovimientoParseado> parsear(byte[] contenido);
}
