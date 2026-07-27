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
 *
 * <p>{@code asientoIdExistente} (F10.3, reconstrucción histórica): si viene
 * seteado y {@code estadoDestino=CONFIRMADO}, la factura se confirma
 * vinculándola a ese asiento YA CONFIRMADO en vez de generar uno nuevo
 * ({@code FacturaVentaService.confirmarVinculandoAsientoExistente}) — nulo
 * en el resto de los casos (flujo normal de F4.6, sin cambios).
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
        @NotBlank String estadoDestino,
        Long asientoIdExistente
) {}
