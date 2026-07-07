package com.montanaritech.contable.contabilidad.cuentacontable;

import com.montanaritech.contable.contabilidad.cuentacontable.dto.CuentaContableResponse;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CuentaContableMapper {

    @Mapping(target = "padreId", source = "padre.id")
    @Mapping(target = "padreCodigo", source = "padre.codigo")
    @Mapping(target = "rubroId", source = "rubro.id")
    @Mapping(target = "rubroNombre", source = "rubro.nombre")
    @Mapping(target = "proyectosUsoHabitual", source = "proyectosUsoHabitual")
    CuentaContableResponse aResponse(CuentaContable c);

    default Set<CuentaContableResponse.ProyectoUsoHabitualDto> mapProyectos(Set<Proyecto> proyectos) {
        if (proyectos == null) {
            return Set.of();
        }
        return proyectos.stream()
                .map(p -> new CuentaContableResponse.ProyectoUsoHabitualDto(p.getId(), p.getNombre()))
                .collect(Collectors.toSet());
    }
}
