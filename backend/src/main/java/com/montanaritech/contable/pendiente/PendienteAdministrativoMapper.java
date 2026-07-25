package com.montanaritech.contable.pendiente;

import com.montanaritech.contable.pendiente.dto.PendienteAdministrativoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PendienteAdministrativoMapper {

    @Mapping(target = "responsableId", source = "responsable.id")
    @Mapping(target = "responsableNombre", source = "responsable.nombre")
    @Mapping(target = "proyectoId", source = "proyecto.id")
    @Mapping(target = "proyectoNombre", source = "proyecto.nombre")
    @Mapping(target = "clienteId", source = "cliente.id")
    @Mapping(target = "clienteNombre", source = "cliente.nombre")
    @Mapping(target = "proveedorId", source = "proveedor.id")
    @Mapping(target = "proveedorNombre", source = "proveedor.nombre")
    PendienteAdministrativoResponse aResponse(PendienteAdministrativo p);
}
