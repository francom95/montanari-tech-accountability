package com.montanaritech.contable.facturacion.importacion.historica.dto;

import com.montanaritech.contable.facturacion.importacion.dto.FilaImportacionPreviewResponse;
import java.util.List;

/**
 * F10.3: envuelve la previsualización de F4.6 ({@link FilaImportacionPreviewResponse},
 * sin cambios) con la decisión de reconstrucción histórica — si la fecha
 * cae en el agujero real (01/10/2025-30/04/2026, sin ningún comprobante
 * individual migrado) o si hay que vincular a un asiento ya existente del
 * Libro Diario (F10.2).
 *
 * <p>{@code asientosCandidatosIds} son los ids resueltos por
 * {@code BuscarAsientoPorComprobante} (fuera del agujero); vacío si está en
 * el agujero (no aplica) o si no hubo ninguna coincidencia.
 *
 * <p>Agujero real verificado: 01/10/2025-30/04/2026 (ver Javadoc de
 * {@code ImportacionFacturaHistoricaService}).
 */
public record FilaImportacionHistoricaPreviewResponse(
        FilaImportacionPreviewResponse base,
        boolean enAgujero,
        List<Long> asientosCandidatosIds,
        String advertenciaMatching
) {}
