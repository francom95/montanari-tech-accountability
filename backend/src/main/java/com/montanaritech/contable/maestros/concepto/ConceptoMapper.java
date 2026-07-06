package com.montanaritech.contable.maestros.concepto;
import com.montanaritech.contable.maestros.concepto.dto.ConceptoResponse;
import org.mapstruct.Mapper;
@Mapper(componentModel = "spring")
public interface ConceptoMapper {
    ConceptoResponse aResponse(Concepto c);
}
