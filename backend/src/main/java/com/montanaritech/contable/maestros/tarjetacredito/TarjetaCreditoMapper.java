package com.montanaritech.contable.maestros.tarjetacredito;

import com.montanaritech.contable.maestros.tarjetacredito.dto.TarjetaCreditoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TarjetaCreditoMapper {

    @Mapping(target = "monedaId", source = "moneda.id")
    @Mapping(target = "monedaCodigo", source = "moneda.codigo")
    @Mapping(target = "cuentaBancariaDebitoId", source = "cuentaBancariaDebito.id")
    @Mapping(target = "cuentaBancariaDebitoAlias", source = "cuentaBancariaDebito.alias")
    @Mapping(target = "cuentaContableId", source = "cuentaContable.id")
    @Mapping(target = "cuentaContableCodigo", source = "cuentaContable.codigo")
    TarjetaCreditoResponse aResponse(TarjetaCredito t);
}
