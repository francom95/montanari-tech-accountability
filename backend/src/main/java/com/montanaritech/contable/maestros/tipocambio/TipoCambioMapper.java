package com.montanaritech.contable.maestros.tipocambio;
import com.montanaritech.contable.maestros.tipocambio.dto.TipoCambioResponse;
import org.mapstruct.Mapper;
@Mapper(componentModel = "spring")
public interface TipoCambioMapper {
    TipoCambioResponse aResponse(TipoCambio tc);
}
