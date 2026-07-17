package com.montanaritech.contable.facturacion.facturaventa.dto;

import com.montanaritech.contable.facturacion.TipoComprobante;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Solo aplica a facturas en BORRADOR (editar una confirmada no está en alcance de F4.2). */
public record FacturaVentaEditarRequest(
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
