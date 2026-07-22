package com.montanaritech.contable.facturacion.facturacompra.dto;

import com.montanaritech.contable.facturacion.TipoComprobante;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FacturaCompraEditarRequest(
        @NotNull Long proveedorId,
        Long proyectoId,
        @NotNull LocalDate fecha,
        LocalDate fechaVencimiento,
        @NotNull TipoComprobante tipoComprobante,
        String puntoVenta,
        @NotBlank String numero,
        @NotNull Long monedaId,
        @NotNull BigDecimal tipoCambio,
        String observaciones,
        @NotEmpty @Valid List<FacturaCompraLineaRequest> lineas,
        @Valid List<FacturaCompraTributoRequest> tributos
) {}
