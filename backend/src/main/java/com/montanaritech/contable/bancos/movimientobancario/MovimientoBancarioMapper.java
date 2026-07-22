package com.montanaritech.contable.bancos.movimientobancario;

import com.montanaritech.contable.bancos.movimientobancario.dto.MovimientoBancarioResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MovimientoBancarioMapper {

    @Mapping(target = "cuentaBancariaId", source = "cuentaBancaria.id")
    @Mapping(target = "cuentaBancariaAlias", source = "cuentaBancaria.alias")
    @Mapping(target = "monedaId", source = "moneda.id")
    @Mapping(target = "monedaCodigo", source = "moneda.codigo")
    @Mapping(target = "cuentaContableSugeridaId", source = "cuentaContableSugerida.id")
    @Mapping(target = "cuentaContableSugeridaCodigo", source = "cuentaContableSugerida.codigo")
    @Mapping(target = "asientoId", source = "asiento.id")
    @Mapping(target = "asientoNumero", source = "asiento.numero")
    MovimientoBancarioResponse aResponse(MovimientoBancario m);
}
