package com.montanaritech.contable.maestros.proyecto;

import com.montanaritech.contable.maestros.proyecto.dto.ProyectoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProyectoMapper {

    @Mapping(target = "clienteId", source = "cliente.id")
    @Mapping(target = "clienteNombre", source = "cliente.nombre")
    @Mapping(target = "responsableId", source = "responsable.id")
    @Mapping(target = "responsableNombre", source = "responsable.nombre")
    @Mapping(target = "monedaId", source = "moneda.id")
    @Mapping(target = "monedaCodigo", source = "moneda.codigo")
    ProyectoResponse aResponse(Proyecto p);

    ProyectoResponse.CuotaResponse aCuotaResponse(ProyectoCuota c);
}
