package com.montanaritech.contable.facturacion.facturacompra;

import com.montanaritech.contable.facturacion.comprobantetributo.ComprobanteTributo;
import com.montanaritech.contable.facturacion.facturacompra.dto.FacturaCompraResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Las percepciones no son una relación JPA de {@link FacturaCompra} (viven en
 * {@code comprobante_tributo}, vinculadas por {@code comprobanteTipo}/{@code
 * comprobanteId}), así que {@code aResponse} toma la lista ya resuelta por
 * {@code FacturaCompraService} como segunda fuente.
 */
@Mapper(componentModel = "spring")
public interface FacturaCompraMapper {

    @Mapping(target = "proveedorId", source = "factura.proveedor.id")
    @Mapping(target = "proveedorNombre", source = "factura.proveedor.nombre")
    @Mapping(target = "proyectoId", source = "factura.proyecto.id")
    @Mapping(target = "proyectoNombre", source = "factura.proyecto.nombre")
    @Mapping(target = "monedaId", source = "factura.moneda.id")
    @Mapping(target = "monedaCodigo", source = "factura.moneda.codigo")
    @Mapping(target = "asientoId", source = "factura.asiento.id")
    @Mapping(target = "asientoNumero", source = "factura.asiento.numero")
    @Mapping(target = "lineas", source = "factura.lineas")
    @Mapping(target = "tributos", source = "tributos")
    FacturaCompraResponse aResponse(FacturaCompra factura, List<ComprobanteTributo> tributos);

    @Mapping(target = "tipoCostoId", source = "tipoCosto.id")
    @Mapping(target = "tipoCostoNombre", source = "tipoCosto.nombre")
    @Mapping(target = "cuentaContableId", source = "cuentaContable.id")
    @Mapping(target = "cuentaContableCodigo", source = "cuentaContable.codigo")
    FacturaCompraResponse.LineaResponse aLineaResponse(FacturaCompraLinea l);

    @Mapping(target = "jurisdiccionId", source = "jurisdiccion.id")
    @Mapping(target = "jurisdiccionNombre", source = "jurisdiccion.nombre")
    FacturaCompraResponse.TributoResponse aTributoResponse(ComprobanteTributo t);
}
