package com.montanaritech.contable.maestros.jurisdiccion;
import com.montanaritech.contable.maestros.jurisdiccion.dto.JurisdiccionResponse;
import org.mapstruct.Mapper;
@Mapper(componentModel = "spring")
public interface JurisdiccionMapper {
    JurisdiccionResponse aResponse(Jurisdiccion j);
}
