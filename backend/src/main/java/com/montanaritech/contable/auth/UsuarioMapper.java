package com.montanaritech.contable.auth;

import com.montanaritech.contable.auth.dto.UsuarioActualResponse;
import com.montanaritech.contable.auth.dto.UsuarioResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    UsuarioResponse aResponse(Usuario usuario);

    UsuarioActualResponse aActualResponse(Usuario usuario);
}
