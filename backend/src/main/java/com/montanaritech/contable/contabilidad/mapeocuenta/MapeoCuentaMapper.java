package com.montanaritech.contable.contabilidad.mapeocuenta;

import com.montanaritech.contable.contabilidad.mapeocuenta.dto.MapeoCuentaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MapeoCuentaMapper {

    @Mapping(target = "cuentaContableId", source = "cuentaContable.id")
    @Mapping(target = "cuentaContableCodigo", source = "cuentaContable.codigo")
    @Mapping(target = "cuentaContableNombre", source = "cuentaContable.nombre")
    MapeoCuentaResponse aResponse(MapeoCuenta mapeoCuenta);
}
