package com.montanaritech.contable.maestros.proyecto.comision;

import com.montanaritech.contable.maestros.proyecto.comision.dto.ComisionProyectoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ComisionProyectoMapper {

    @Mapping(target = "proyectoId", source = "proyecto.id")
    @Mapping(target = "proyectoNombre", source = "proyecto.nombre")
    @Mapping(target = "comisionistaId", source = "comisionista.id")
    @Mapping(target = "comisionistaNombre", source = "comisionista.nombre")
    @Mapping(target = "monedaId", source = "moneda.id")
    @Mapping(target = "monedaCodigo", source = "moneda.codigo")
    ComisionProyectoResponse aResponse(ComisionProyecto c);
}
