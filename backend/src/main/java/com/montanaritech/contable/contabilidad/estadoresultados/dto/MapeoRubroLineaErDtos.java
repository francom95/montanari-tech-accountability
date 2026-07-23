package com.montanaritech.contable.contabilidad.estadoresultados.dto;

import com.montanaritech.contable.contabilidad.estadoresultados.LineaEstadoResultados;
import com.montanaritech.contable.maestros.categoria.Categoria;
import jakarta.validation.constraints.NotNull;

public final class MapeoRubroLineaErDtos {

    private MapeoRubroLineaErDtos() {
    }

    public record CrearRequest(
            @NotNull Long rubroId,
            @NotNull Categoria.TipoCategoria naturaleza,
            @NotNull LineaEstadoResultados linea) {}

    public record EditarRequest(@NotNull LineaEstadoResultados linea) {}

    public record Response(
            Long id,
            Long rubroId,
            String rubroNombre,
            Categoria.TipoCategoria naturaleza,
            LineaEstadoResultados linea) {}
}
