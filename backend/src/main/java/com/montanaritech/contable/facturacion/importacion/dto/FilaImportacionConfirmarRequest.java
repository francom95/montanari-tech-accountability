package com.montanaritech.contable.facturacion.importacion.dto;

import com.montanaritech.contable.facturacion.TipoComprobante;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fila del formulario de carga asistida, ya revisada/corregida por el
 * usuario (F4.6). Trae exactamente uno de {@code clienteId}/{@code
 * proveedorId} YA resuelto, o los datos de alta rápida
 * ({@code altaRapidaNombre}/{@code altaRapidaCuit}/{@code
 * altaRapidaJurisdiccionId}) para crear el cliente/proveedor en el mismo
 * paso — nunca ambos vacíos.
 */
public record FilaImportacionConfirmarRequest(
        @NotBlank String nombreArchivo,
        @NotBlank String tipo,
        Long clienteId,
        Long proveedorId,
        String altaRapidaNombre,
        String altaRapidaCuit,
        Long altaRapidaJurisdiccionId,
        Long proyectoId,
        @NotNull LocalDate fecha,
        LocalDate fechaVencimiento,
        @NotNull TipoComprobante tipoComprobante,
        String puntoVenta,
        @NotBlank String numero,
        @NotNull Long monedaId,
        @NotNull BigDecimal tipoCambio,
        String observaciones,
        @NotBlank String descripcionLinea,
        @NotNull @DecimalMin("0.01") BigDecimal importeNeto,
        @NotNull BigDecimal alicuotaIva,
        String tipoIngreso,
        Long tipoCostoId,
        @NotBlank String estadoDestino
) {}
