package com.montanaritech.contable.facturacion.cobro;

import com.montanaritech.contable.facturacion.comprobantetributo.ComprobanteTributo;
import com.montanaritech.contable.facturacion.cobro.dto.CobroResponse;
import java.math.BigDecimal;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Las retenciones no son una relación JPA de {@link Cobro} (viven en
 * {@code comprobante_tributo}) y {@code montoAnticipoDisponible} es un valor
 * calculado por {@code CobroService} (no persistido) — {@code aResponse}
 * toma ambos ya resueltos como fuentes adicionales.
 */
@Mapper(componentModel = "spring")
public interface CobroMapper {

    @Mapping(target = "clienteId", source = "cobro.cliente.id")
    @Mapping(target = "clienteNombre", source = "cobro.cliente.nombre")
    @Mapping(target = "fecha", source = "cobro.fecha")
    @Mapping(target = "monedaId", source = "cobro.moneda.id")
    @Mapping(target = "monedaCodigo", source = "cobro.moneda.codigo")
    @Mapping(target = "tipoCambio", source = "cobro.tipoCambio")
    @Mapping(target = "cuentaBancariaId", source = "cobro.cuentaBancaria.id")
    @Mapping(target = "cuentaBancariaAlias", source = "cobro.cuentaBancaria.alias")
    @Mapping(target = "total", source = "cobro.total")
    @Mapping(target = "totalArs", source = "cobro.totalArs")
    @Mapping(target = "importeRetenciones", source = "cobro.importeRetenciones")
    @Mapping(target = "montoAnticipo", source = "cobro.montoAnticipo")
    @Mapping(target = "montoAnticipoDisponible", source = "montoAnticipoDisponible")
    @Mapping(target = "estado", source = "cobro.estado")
    @Mapping(target = "asientoId", source = "cobro.asiento.id")
    @Mapping(target = "asientoNumero", source = "cobro.asiento.numero")
    @Mapping(target = "observaciones", source = "cobro.observaciones")
    @Mapping(target = "lineas", source = "cobro.lineas")
    @Mapping(target = "tributos", source = "tributos")
    @Mapping(target = "aplicacionesAnticipo", source = "aplicaciones")
    CobroResponse aResponse(Cobro cobro, List<ComprobanteTributo> tributos, List<AplicacionAnticipoCliente> aplicaciones, BigDecimal montoAnticipoDisponible);

    @Mapping(target = "facturaVentaId", source = "facturaVenta.id")
    @Mapping(target = "facturaVentaNumero", source = "facturaVenta.numero")
    CobroResponse.ImputacionResponse aImputacionResponse(CobroImputacion l);

    CobroResponse.TributoResponse aTributoResponse(ComprobanteTributo t);

    @Mapping(target = "facturaVentaId", source = "facturaVenta.id")
    @Mapping(target = "facturaVentaNumero", source = "facturaVenta.numero")
    @Mapping(target = "asientoId", source = "asiento.id")
    @Mapping(target = "asientoNumero", source = "asiento.numero")
    CobroResponse.AplicacionAnticipoResponse aAplicacionResponse(AplicacionAnticipoCliente a);
}
