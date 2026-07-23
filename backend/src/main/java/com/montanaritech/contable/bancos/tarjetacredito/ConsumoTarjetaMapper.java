package com.montanaritech.contable.bancos.tarjetacredito;

import com.montanaritech.contable.bancos.tarjetacredito.dto.ConsumoTarjetaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ConsumoTarjetaMapper {

    @Mapping(target = "tarjetaCreditoId", source = "tarjetaCredito.id")
    @Mapping(target = "monedaId", source = "moneda.id")
    @Mapping(target = "monedaCodigo", source = "moneda.codigo")
    @Mapping(target = "cuentaContableId", source = "cuentaContable.id")
    @Mapping(target = "cuentaContableCodigo", source = "cuentaContable.codigo")
    @Mapping(target = "cuentaContableNombre", source = "cuentaContable.nombre")
    @Mapping(target = "proveedorId", source = "proveedor.id")
    @Mapping(target = "proveedorNombre", source = "proveedor.nombre")
    @Mapping(target = "proyectoId", source = "proyecto.id")
    @Mapping(target = "proyectoNombre", source = "proyecto.nombre")
    @Mapping(target = "conceptoId", source = "concepto.id")
    @Mapping(target = "conceptoNombre", source = "concepto.nombre")
    ConsumoTarjetaResponse aResponse(ConsumoTarjeta c);
}
