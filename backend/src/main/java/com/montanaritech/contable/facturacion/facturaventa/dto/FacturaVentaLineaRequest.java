package com.montanaritech.contable.facturacion.facturaventa.dto;

import com.montanaritech.contable.facturacion.facturaventa.TipoIngreso;
import com.montanaritech.contable.facturacion.facturaventa.TipoLineaFactura;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * {@code importeIva} no se pide: el service lo calcula
 * ({@code round2(importeNeto × alicuotaIva / 100)}, F4.1). {@code alicuotaIva}
 * se valida contra el catálogo AFIP (0, 2.5, 5, 10.5, 21, 27).
 */
public record FacturaVentaLineaRequest(
        @NotBlank String descripcion,
        @NotNull TipoLineaFactura tipo,
        @NotNull @DecimalMin(value = "0.01") BigDecimal importeNeto,
        @NotNull BigDecimal alicuotaIva,
        TipoIngreso tipoIngreso,
        Long cuentaContableId
) {}
