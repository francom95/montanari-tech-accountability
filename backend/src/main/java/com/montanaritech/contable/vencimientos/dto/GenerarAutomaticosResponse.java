package com.montanaritech.contable.vencimientos.dto;

import java.util.List;

public record GenerarAutomaticosResponse(List<GeneradoPorTipo> generados, int total) {

    public record GeneradoPorTipo(String origen, int cantidad) {}
}
