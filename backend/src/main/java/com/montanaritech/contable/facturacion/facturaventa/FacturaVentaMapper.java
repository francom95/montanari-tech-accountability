package com.montanaritech.contable.facturacion.facturaventa;

import com.montanaritech.contable.facturacion.facturaventa.dto.FacturaVentaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FacturaVentaMapper {

    @Mapping(target = "clienteId", source = "cliente.id")
    @Mapping(target = "clienteNombre", source = "cliente.nombre")
    @Mapping(target = "proyectoId", source = "proyecto.id")
    @Mapping(target = "proyectoNombre", source = "proyecto.nombre")
    @Mapping(target = "jurisdiccionDestinoId", source = "jurisdiccionDestino.id")
    @Mapping(target = "monedaId", source = "moneda.id")
    @Mapping(target = "monedaCodigo", source = "moneda.codigo")
    @Mapping(target = "asientoId", source = "asiento.id")
    @Mapping(target = "asientoNumero", source = "asiento.numero")
    FacturaVentaResponse aResponse(FacturaVenta f);

    @Mapping(target = "cuentaContableId", source = "cuentaContable.id")
    @Mapping(target = "cuentaContableCodigo", source = "cuentaContable.codigo")
    FacturaVentaResponse.LineaResponse aLineaResponse(FacturaVentaLinea l);
}
