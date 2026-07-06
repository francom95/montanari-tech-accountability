package com.montanaritech.contable.maestros.rubro;

import com.montanaritech.contable.maestros.rubro.dto.RubroResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RubroMapper {

    @Mapping(target = "categoriaId", source = "categoria.id")
    RubroResponse aResponse(Rubro r);
}
