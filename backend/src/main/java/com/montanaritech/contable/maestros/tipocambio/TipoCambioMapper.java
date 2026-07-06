package com.montanaritech.contable.maestros.tipocambio;

import com.montanaritech.contable.maestros.tipocambio.dto.TipoCambioResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TipoCambioMapper {

    @Mapping(target = "monedaId", source = "moneda.id")
    TipoCambioResponse aResponse(TipoCambio tc);
}
