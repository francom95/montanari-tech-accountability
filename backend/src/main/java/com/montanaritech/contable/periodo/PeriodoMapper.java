package com.montanaritech.contable.periodo;

import com.montanaritech.contable.periodo.dto.PeriodoDtos.PeriodoResponse;
import org.springframework.stereotype.Component;

@Component
public class PeriodoMapper {

    public PeriodoResponse aResponse(Periodo p) {
        return new PeriodoResponse(p.getId(), p.getAnio(), p.getMes(), p.getEstado(), p.getMotivoCierre(), p.getMotivoReapertura());
    }
}
