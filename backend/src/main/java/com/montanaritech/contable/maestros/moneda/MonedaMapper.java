package com.montanaritech.contable.maestros.moneda;

import com.montanaritech.contable.maestros.moneda.dto.MonedaResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MonedaMapper {

    MonedaResponse aResponse(Moneda moneda);
}
