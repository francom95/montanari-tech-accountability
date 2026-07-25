package com.montanaritech.contable.busqueda.dto;

import com.montanaritech.contable.busqueda.TipoEntidadBusqueda;
import java.util.Map;

public record BusquedaGlobalResponse(Map<TipoEntidadBusqueda, GrupoResultado> grupos) {}
