package com.montanaritech.contable.facturacion.importacion;

import com.montanaritech.contable.facturacion.TipoComprobante;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Resultado de la extracción básica de texto de un PDF (F4.6). Best-effort:
 * cualquier campo puede venir {@code null} si el layout del comprobante no
 * coincide con los patrones conocidos — el formulario de carga asistida es
 * el que completa lo que falte, nunca se confirma nada automáticamente.
 */
public record CamposExtraidosPdf(
        String tipoSugerido,
        TipoComprobante tipoComprobante,
        String puntoVenta,
        String numero,
        LocalDate fecha,
        String cuitContraparte,
        String monedaCodigo,
        BigDecimal tipoCambio,
        BigDecimal netoGravado,
        BigDecimal alicuotaIva,
        BigDecimal total,
        String cae,
        List<String> advertencias
) {}
