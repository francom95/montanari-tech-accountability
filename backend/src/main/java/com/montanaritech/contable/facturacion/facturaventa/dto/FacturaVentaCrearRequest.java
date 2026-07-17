package com.montanaritech.contable.facturacion.facturaventa.dto;

import com.montanaritech.contable.facturacion.TipoComprobante;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Crea siempre un BORRADOR (F3.1 §3.5, mismo criterio que
 * {@code AsientoCrearRequest}): la consistencia neto+IVA=total y la
 * resolución de cuentas se validan recién al confirmar.
 */
public record FacturaVentaCrearRequest(
        @NotNull Long clienteId,
        Long proyectoId,
        @NotNull LocalDate fecha,
        LocalDate fechaVencimiento,
        @NotNull TipoComprobante tipoComprobante,
        String puntoVenta,
        @NotBlank String numero,
        Long jurisdiccionDestinoId,
        @NotNull Long monedaId,
        @NotNull BigDecimal tipoCambio,
        String observaciones,
        @NotEmpty @Valid List<FacturaVentaLineaRequest> lineas
) {}
