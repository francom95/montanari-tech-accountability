package com.montanaritech.contable.pendiente.dto;

import com.montanaritech.contable.pendiente.EstadoPendiente;
import com.montanaritech.contable.pendiente.PrioridadPendiente;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record PendienteAdministrativoEditarRequest(
        @NotBlank String titulo,
        String descripcion,
        LocalDate fechaEstimadaResolucion,
        @NotNull PrioridadPendiente prioridad,
        @NotNull EstadoPendiente estado,
        Long responsableId,
        String categoria,
        Long proyectoId,
        Long clienteId,
        Long proveedorId,
        String observaciones
) {}
