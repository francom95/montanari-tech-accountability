package com.montanaritech.contable.common.audit;

import com.montanaritech.contable.common.audit.dto.AuditoriaLogResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditoriaLogMapper {

    AuditoriaLogResponse aResponse(AuditoriaLog log);
}
