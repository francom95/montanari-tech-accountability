package com.montanaritech.contable.maestros.concepto;

import com.montanaritech.contable.maestros.concepto.dto.ConceptoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ConceptoMapper {

    @Mapping(target = "monedaId", source = "moneda.id")
    ConceptoResponse aResponse(Concepto c);
}
