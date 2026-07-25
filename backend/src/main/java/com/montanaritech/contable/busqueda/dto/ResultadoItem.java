package com.montanaritech.contable.busqueda.dto;

import com.montanaritech.contable.busqueda.TipoEntidadBusqueda;
import java.time.LocalDate;

/**
 * Resultado mínimo para armar el link en el frontend (F9.2) — el mapa de
 * rutas por tipo vive en el cliente, no acá. {@code contextoId} solo se usa
 * para {@code ETAPA} (id del proyecto padre, para navegar a
 * {@code /proyectos/{contextoId}?tab=etapas&id={id}}); null para el resto.
 */
public record ResultadoItem(
        Long id,
        TipoEntidadBusqueda tipo,
        String resumen,
        LocalDate fecha,
        Long contextoId
) {}
