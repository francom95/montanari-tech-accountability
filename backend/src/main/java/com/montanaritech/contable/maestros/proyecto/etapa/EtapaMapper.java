package com.montanaritech.contable.maestros.proyecto.etapa;

import com.montanaritech.contable.maestros.proveedor.Proveedor;
import com.montanaritech.contable.maestros.proyecto.etapa.dto.EtapaResponse;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EtapaMapper {

    @Mapping(target = "proyectoId", source = "proyecto.id")
    @Mapping(target = "proveedores", source = "proveedores")
    EtapaResponse aResponse(Etapa e);

    default Set<EtapaResponse.ProveedorDto> mapProveedores(Set<Proveedor> proveedores) {
        if (proveedores == null) {
            return Set.of();
        }
        return proveedores.stream()
                .map(pr -> new EtapaResponse.ProveedorDto(pr.getId(), pr.getNombre()))
                .collect(Collectors.toSet());
    }
}
