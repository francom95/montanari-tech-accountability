package com.montanaritech.contable.maestros.proveedor;

import com.montanaritech.contable.maestros.proveedor.dto.ProveedorResponse;
import com.montanaritech.contable.maestros.tipocosto.TipoCosto;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProveedorMapper {
    @Mapping(target = "jurisdiccionId", source = "jurisdiccion.id")
    @Mapping(target = "jurisdiccionNombre", source = "jurisdiccion.nombre")
    @Mapping(target = "monedaHabitualId", source = "monedaHabitual.id")
    @Mapping(target = "monedaHabitualCodigo", source = "monedaHabitual.codigo")
    @Mapping(target = "tiposCosto", source = "tiposCosto")
    ProveedorResponse aResponse(Proveedor p);

    default Set<ProveedorResponse.TipoCostoDto> mapTiposCosto(Set<TipoCosto> tiposCosto) {
        if (tiposCosto == null) {
            return Set.of();
        }
        return tiposCosto.stream()
                .map(tc -> new ProveedorResponse.TipoCostoDto(tc.getId(), tc.getNombre()))
                .collect(Collectors.toSet());
    }
}
