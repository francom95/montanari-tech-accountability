package com.montanaritech.contable.impuestos.iibb;

import com.montanaritech.contable.impuestos.iibb.dto.LiquidacionIibbDtos.ComponenteResponse;
import com.montanaritech.contable.impuestos.iibb.dto.LiquidacionIibbDtos.JurisdiccionPrevisualizadaResponse;
import com.montanaritech.contable.impuestos.iibb.dto.LiquidacionIibbDtos.JurisdiccionResponse;
import com.montanaritech.contable.impuestos.iibb.dto.LiquidacionIibbDtos.LiquidacionResponse;
import com.montanaritech.contable.impuestos.iibb.dto.LiquidacionIibbDtos.PrevisualizacionResponse;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Mapper a mano (como el de IVA): la respuesta lleva campos derivados
 * ({@code importeFinal}, {@code aporte}) y advertencias que no viven en la
 * entidad sino que llegan del motor de cálculo.
 */
@Component
public class LiquidacionIibbMapper {

    public LiquidacionResponse aResponse(LiquidacionIibb l, List<String> advertencias) {
        return new LiquidacionResponse(
                l.getId(), l.getAnio(), l.getMes(), l.getFechaDesde(), l.getFechaHasta(),
                l.getEstado().name(), l.getBaseTotal(), l.getSaldoAPagarTotal(), l.getSaldoAFavorTotal(),
                l.getAsiento() != null ? l.getAsiento().getId() : null,
                l.getAsiento() != null ? l.getAsiento().getNumero() : null,
                l.getObservaciones(),
                l.getJurisdicciones().stream().map(this::aJurisdiccionResponse).toList(),
                advertencias);
    }

    private JurisdiccionResponse aJurisdiccionResponse(LiquidacionIibbJurisdiccion j) {
        return new JurisdiccionResponse(
                j.getId(), j.getJurisdiccion().getId(), j.getJurisdiccion().getCodigo(), j.getJurisdiccion().getNombre(),
                j.getCoeficiente(), j.getBaseImponible(), j.getAlicuota(), j.getImpuestoDeterminado(),
                j.getSaldoAPagar(), j.getSaldoAFavor(), j.getOrden(),
                j.getComponentes().stream().map(this::aComponenteResponse).toList());
    }

    private ComponenteResponse aComponenteResponse(LiquidacionIibbComponente c) {
        return new ComponenteResponse(
                c.getId(), c.getTipo(), c.getDescripcion(),
                c.getImporteCalculado(), c.getImporteAjuste(), c.getImporteFinal(), c.getAporte(),
                c.getMotivoAjuste(),
                c.getCuentaContable() != null ? c.getCuentaContable().getId() : null,
                c.getCuentaContable() != null ? c.getCuentaContable().getCodigo() : null,
                c.getCuentaContable() != null ? c.getCuentaContable().getNombre() : null,
                c.isManual(), c.getOrden());
    }

    public PrevisualizacionResponse aPrevisualizacion(CalculoIibb calculo) {
        return new PrevisualizacionResponse(
                calculo.anio(), calculo.mes(), calculo.fechaDesde(), calculo.fechaHasta(),
                calculo.baseTotal(), calculo.deduccionesDisponibles(),
                calculo.jurisdicciones().stream()
                        .map(jc -> new JurisdiccionPrevisualizadaResponse(
                                jc.jurisdiccionId(), jc.jurisdiccionCodigo(), jc.jurisdiccionNombre(),
                                jc.ventasDestino(), jc.coeficiente(), jc.baseImponible(),
                                jc.alicuota(), jc.impuestoDeterminado(), jc.saldoAFavorAnterior()))
                        .toList(),
                calculo.advertencias());
    }
}
