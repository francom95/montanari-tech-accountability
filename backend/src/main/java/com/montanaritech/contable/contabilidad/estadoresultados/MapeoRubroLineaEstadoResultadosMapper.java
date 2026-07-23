package com.montanaritech.contable.contabilidad.estadoresultados;

import com.montanaritech.contable.contabilidad.estadoresultados.dto.MapeoRubroLineaErDtos.Response;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MapeoRubroLineaEstadoResultadosMapper {

    @Mapping(target = "rubroId", source = "rubro.id")
    @Mapping(target = "rubroNombre", source = "rubro.nombre")
    Response aResponse(MapeoRubroLineaEstadoResultados mapeo);
}
