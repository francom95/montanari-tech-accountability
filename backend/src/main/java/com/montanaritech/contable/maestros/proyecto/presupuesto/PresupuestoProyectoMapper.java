package com.montanaritech.contable.maestros.proyecto.presupuesto;

import com.montanaritech.contable.maestros.proyecto.presupuesto.dto.PresupuestoProyectoDtos.LineaCostoResponse;
import com.montanaritech.contable.maestros.proyecto.presupuesto.dto.PresupuestoProyectoDtos.Response;
import org.springframework.stereotype.Component;

/**
 * Combina la entidad (inputs persistidos) con el resultado recién calculado
 * ({@link PresupuestoCalculado} nunca se persiste) — no es un mapeo directo
 * de campos, por eso es manual y no MapStruct.
 */
@Component
public class PresupuestoProyectoMapper {

    public Response aResponse(PresupuestoProyecto presupuesto, PresupuestoCalculado calculado) {
        return new Response(
                presupuesto.getId(),
                presupuesto.getProyecto().getId(),
                presupuesto.getMargenDeseadoUsd(),
                presupuesto.getComisionesBancariasIntermediasComexUsd(),
                presupuesto.getObservaciones(),
                presupuesto.getLineasCosto().stream()
                        .map(l -> new LineaCostoResponse(l.getId(), l.getNombre(), l.getImporteUsd()))
                        .toList(),
                calculado);
    }
}
