package com.montanaritech.contable.maestros.proyecto.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProyectoResponse(
        Long id,
        String nombre,
        Long clienteId,
        String clienteNombre,
        Long responsableId,
        String responsableNombre,
        String pais,
        String tipoProyecto,
        String estado,
        Long monedaId,
        String monedaCodigo,
        BigDecimal montoTotal,
        Integer cantidadPagosPactados,
        String comentarios,
        String estadoComercial,
        String estadoFacturacion,
        String estadoCobranza,
        LocalDate fechaEstimadaFinalizacion,
        LocalDate fechaRealFinalizacion,
        List<CuotaResponse> cuotas,
        boolean activo
) {
    public record CuotaResponse(Long id, Integer numero, LocalDate fechaEstimadaCobro, BigDecimal importe) {}
}
