package com.montanaritech.contable.facturacion.importacion.dto;

import com.montanaritech.contable.facturacion.TipoComprobante;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Previsualización de una factura histórica extraída de un PDF (F4.6).
 * Ningún campo llega validado ni persistido — es lo que el extractor pudo
 * detectar más la resolución de cliente/proveedor por CUIT, para
 * pre-completar el formulario de carga asistida que el usuario revisa
 * antes de confirmar.
 */
public record FilaImportacionPreviewResponse(
        String nombreArchivo,
        String tipoSugerido,
        TipoComprobante tipoComprobante,
        String puntoVenta,
        String numero,
        LocalDate fecha,
        String cuitContraparte,
        Long clienteId,
        String clienteNombre,
        Long proveedorId,
        String proveedorNombre,
        String monedaCodigo,
        Long monedaId,
        BigDecimal tipoCambio,
        BigDecimal netoGravado,
        BigDecimal alicuotaIva,
        BigDecimal total,
        String cae,
        List<String> advertencias,
        String textoExtraido
) {}
