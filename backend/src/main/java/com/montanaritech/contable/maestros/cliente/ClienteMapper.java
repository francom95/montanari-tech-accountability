package com.montanaritech.contable.maestros.cliente;

import com.montanaritech.contable.maestros.cliente.dto.ClienteResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClienteMapper {
    @Mapping(target = "jurisdiccionId", source = "jurisdiccion.id")
    @Mapping(target = "jurisdiccionNombre", source = "jurisdiccion.nombre")
    @Mapping(target = "cuentaCxcId", source = "cuentaCxc.id")
    @Mapping(target = "cuentaCxcCodigo", source = "cuentaCxc.codigo")
    ClienteResponse aResponse(Cliente c);
}
