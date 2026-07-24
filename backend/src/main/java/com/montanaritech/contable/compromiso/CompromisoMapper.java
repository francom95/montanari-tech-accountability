package com.montanaritech.contable.compromiso;

import com.montanaritech.contable.compromiso.dto.CompromisoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CompromisoMapper {

    @Mapping(target = "monedaId", source = "moneda.id")
    @Mapping(target = "monedaCodigo", source = "moneda.codigo")
    @Mapping(target = "proveedorId", source = "proveedor.id")
    @Mapping(target = "proveedorNombre", source = "proveedor.nombre")
    @Mapping(target = "proyectoId", source = "proyecto.id")
    @Mapping(target = "proyectoNombre", source = "proyecto.nombre")
    CompromisoResponse aResponse(Compromiso c);
}
