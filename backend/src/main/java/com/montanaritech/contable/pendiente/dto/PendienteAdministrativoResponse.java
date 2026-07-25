package com.montanaritech.contable.pendiente.dto;

import com.montanaritech.contable.pendiente.EstadoPendiente;
import com.montanaritech.contable.pendiente.PrioridadPendiente;
import java.time.LocalDate;

public record PendienteAdministrativoResponse(
        Long id,
        String titulo,
        String descripcion,
        LocalDate fechaEstimadaResolucion,
        PrioridadPendiente prioridad,
        EstadoPendiente estado,
        Long responsableId,
        String responsableNombre,
        String categoria,
        Long proyectoId,
        String proyectoNombre,
        Long clienteId,
        String clienteNombre,
        Long proveedorId,
        String proveedorNombre,
        String observaciones,
        boolean activo
) {}
