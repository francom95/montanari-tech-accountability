package com.montanaritech.contable.contabilidad.asiento;

import com.montanaritech.contable.contabilidad.asiento.dto.AsientoResponse;
import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AsientoMapper {

    @Mapping(target = "totalDebe", expression = "java(totalDebe(a))")
    @Mapping(target = "totalHaber", expression = "java(totalHaber(a))")
    AsientoResponse aResponse(Asiento a);

    @Mapping(target = "cuentaContableId", source = "cuentaContable.id")
    @Mapping(target = "cuentaContableCodigo", source = "cuentaContable.codigo")
    @Mapping(target = "cuentaContableNombre", source = "cuentaContable.nombre")
    @Mapping(target = "monedaId", source = "moneda.id")
    @Mapping(target = "monedaCodigo", source = "moneda.codigo")
    @Mapping(target = "proyectoId", source = "proyecto.id")
    @Mapping(target = "proyectoNombre", source = "proyecto.nombre")
    @Mapping(target = "etapaId", source = "etapa.id")
    @Mapping(target = "etapaNombre", source = "etapa.nombre")
    @Mapping(target = "clienteId", source = "cliente.id")
    @Mapping(target = "clienteNombre", source = "cliente.nombre")
    @Mapping(target = "proveedorId", source = "proveedor.id")
    @Mapping(target = "proveedorNombre", source = "proveedor.nombre")
    @Mapping(target = "cuentaBancariaId", source = "cuentaBancaria.id")
    @Mapping(target = "cuentaBancariaAlias", source = "cuentaBancaria.alias")
    AsientoResponse.LineaResponse aLineaResponse(AsientoLinea l);

    default BigDecimal totalDebe(Asiento a) {
        return a.getLineas().stream().map(AsientoLinea::getDebe).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    default BigDecimal totalHaber(Asiento a) {
        return a.getLineas().stream().map(AsientoLinea::getHaber).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
