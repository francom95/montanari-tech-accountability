package com.montanaritech.contable.common.reporte;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Título y filtros aplicados de un reporte exportable (F7.1, PL-3). Los
 * filtros van en el encabezado del Excel/PDF para que el archivo sea
 * autodescriptivo aunque se abra fuera de la pantalla que lo generó.
 */
public record ContextoReporte(String titulo, List<String> filtrosAplicados) {

    /**
     * @param filtros descripciones ya formateadas (p. ej. {@code "Desde: 01/03/2026"});
     *                los {@code null} se descartan, para que cada controller pase
     *                sus filtros opcionales sin chequear él mismo cuáles vinieron.
     */
    public static ContextoReporte de(String titulo, String... filtros) {
        return new ContextoReporte(titulo, Arrays.stream(filtros).filter(Objects::nonNull).toList());
    }
}
