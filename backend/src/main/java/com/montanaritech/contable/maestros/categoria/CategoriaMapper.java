package com.montanaritech.contable.maestros.categoria;
import com.montanaritech.contable.maestros.categoria.dto.CategoriaResponse;
import org.mapstruct.Mapper;
@Mapper(componentModel = "spring")
public interface CategoriaMapper {
    CategoriaResponse aResponse(Categoria c);
}
