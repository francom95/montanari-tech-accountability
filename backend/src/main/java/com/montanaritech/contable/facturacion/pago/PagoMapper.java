package com.montanaritech.contable.facturacion.pago;

import com.montanaritech.contable.facturacion.pago.dto.PagoResponse;
import java.math.BigDecimal;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** {@code montoAnticipoDisponible} es un valor calculado por {@code PagoService} (no persistido). */
@Mapper(componentModel = "spring")
public interface PagoMapper {

    @Mapping(target = "proveedorId", source = "pago.proveedor.id")
    @Mapping(target = "proveedorNombre", source = "pago.proveedor.nombre")
    @Mapping(target = "fecha", source = "pago.fecha")
    @Mapping(target = "monedaId", source = "pago.moneda.id")
    @Mapping(target = "monedaCodigo", source = "pago.moneda.codigo")
    @Mapping(target = "tipoCambio", source = "pago.tipoCambio")
    @Mapping(target = "cuentaBancariaId", source = "pago.cuentaBancaria.id")
    @Mapping(target = "cuentaBancariaAlias", source = "pago.cuentaBancaria.alias")
    @Mapping(target = "total", source = "pago.total")
    @Mapping(target = "totalArs", source = "pago.totalArs")
    @Mapping(target = "montoAnticipo", source = "pago.montoAnticipo")
    @Mapping(target = "montoAnticipoDisponible", source = "montoAnticipoDisponible")
    @Mapping(target = "estado", source = "pago.estado")
    @Mapping(target = "asientoId", source = "pago.asiento.id")
    @Mapping(target = "asientoNumero", source = "pago.asiento.numero")
    @Mapping(target = "observaciones", source = "pago.observaciones")
    @Mapping(target = "lineas", source = "pago.lineas")
    @Mapping(target = "aplicacionesAnticipo", source = "aplicaciones")
    PagoResponse aResponse(Pago pago, List<AplicacionAnticipoProveedor> aplicaciones, BigDecimal montoAnticipoDisponible);

    @Mapping(target = "facturaCompraId", source = "facturaCompra.id")
    @Mapping(target = "facturaCompraNumero", source = "facturaCompra.numero")
    PagoResponse.ImputacionResponse aImputacionResponse(PagoImputacion l);

    @Mapping(target = "facturaCompraId", source = "facturaCompra.id")
    @Mapping(target = "facturaCompraNumero", source = "facturaCompra.numero")
    @Mapping(target = "asientoId", source = "asiento.id")
    @Mapping(target = "asientoNumero", source = "asiento.numero")
    PagoResponse.AplicacionAnticipoResponse aAplicacionResponse(AplicacionAnticipoProveedor a);
}
