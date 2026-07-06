package com.montanaritech.contable.maestros.cliente;

import com.montanaritech.contable.maestros.cliente.dto.ClienteResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClienteMapper {
    @Mapping(target = "jurisdiccionId", source = "jurisdiccion.id")
    @Mapping(target = "jurisdiccionNombre", source = "jurisdiccion.nombre")
    ClienteResponse aResponse(Cliente c);
}
