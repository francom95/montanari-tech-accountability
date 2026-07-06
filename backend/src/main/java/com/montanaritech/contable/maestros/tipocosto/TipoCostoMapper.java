package com.montanaritech.contable.maestros.tipocosto;
import com.montanaritech.contable.maestros.tipocosto.dto.TipoCostoResponse;
import org.mapstruct.Mapper;
@Mapper(componentModel = "spring")
public interface TipoCostoMapper {
    TipoCostoResponse aResponse(TipoCosto tc);
}
