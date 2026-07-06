package com.montanaritech.contable.maestros.cuentabancaria;

import com.montanaritech.contable.maestros.cuentabancaria.dto.CuentaBancariaResponse;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CuentaBancariaMapper {

    @Mapping(target = "monedaId", source = "moneda.id")
    @Mapping(target = "monedaCodigo", source = "moneda.codigo")
    CuentaBancariaResponse aResponse(CuentaBancaria c);
}
