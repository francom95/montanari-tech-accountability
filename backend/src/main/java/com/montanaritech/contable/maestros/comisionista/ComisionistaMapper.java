package com.montanaritech.contable.maestros.comisionista;

import com.montanaritech.contable.maestros.comisionista.dto.ComisionistaResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ComisionistaMapper {
    ComisionistaResponse aResponse(Comisionista c);
}
