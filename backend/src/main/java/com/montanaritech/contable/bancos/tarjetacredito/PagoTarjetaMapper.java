package com.montanaritech.contable.bancos.tarjetacredito;

import com.montanaritech.contable.bancos.tarjetacredito.dto.PagoTarjetaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PagoTarjetaMapper {

    @Mapping(target = "tarjetaCreditoId", source = "tarjetaCredito.id")
    @Mapping(target = "tarjetaCreditoEntidad", source = "tarjetaCredito.entidad")
    @Mapping(target = "monedaId", source = "moneda.id")
    @Mapping(target = "monedaCodigo", source = "moneda.codigo")
    @Mapping(target = "estado", source = "estado")
    @Mapping(target = "asientoId", source = "asiento.id")
    @Mapping(target = "asientoNumero", source = "asiento.numero")
    PagoTarjetaResponse aResponse(PagoTarjeta p);
}
