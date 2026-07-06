package com.montanaritech.contable.maestros.rubro;
import com.montanaritech.contable.maestros.rubro.dto.RubroResponse;
import org.mapstruct.Mapper;
@Mapper(componentModel = "spring")
public interface RubroMapper {
    RubroResponse aResponse(Rubro r);
}
