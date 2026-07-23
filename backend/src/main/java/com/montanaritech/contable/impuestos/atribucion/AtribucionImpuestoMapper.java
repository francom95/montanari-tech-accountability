package com.montanaritech.contable.impuestos.atribucion;

import com.montanaritech.contable.impuestos.atribucion.dto.AtribucionImpuestoDtos.AtribucionResponse;
import com.montanaritech.contable.impuestos.atribucion.dto.AtribucionImpuestoDtos.LineaResponse;
import org.springframework.stereotype.Component;

/** Mapper a mano: la respuesta se arma desde {@link CalculoAtribucion} (guardado o previsualizado). */
@Component
public class AtribucionImpuestoMapper {

    public AtribucionResponse aResponse(CalculoAtribucion c) {
        return new AtribucionResponse(
                c.liquidacionTipo(), c.liquidacionId(), c.anio(), c.mes(), c.criterio(),
                c.montoTotal(), c.guardada(),
                c.lineas().stream()
                        .map(l -> new LineaResponse(l.proyectoId(), l.proyectoNombre(), l.porcentaje(), l.monto()))
                        .toList(),
                c.advertencias());
    }
}
