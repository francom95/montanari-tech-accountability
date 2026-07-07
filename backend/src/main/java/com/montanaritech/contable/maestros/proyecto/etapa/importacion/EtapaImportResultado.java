package com.montanaritech.contable.maestros.proyecto.etapa.importacion;

import com.montanaritech.contable.maestros.proyecto.etapa.dto.EtapaResponse;
import java.util.List;

public record EtapaImportResultado(List<EtapaResponse> creadas, List<EtapaImportFilaDto> rechazadas) {}
