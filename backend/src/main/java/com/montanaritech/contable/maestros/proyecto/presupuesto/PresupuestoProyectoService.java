package com.montanaritech.contable.maestros.proyecto.presupuesto;

import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import com.montanaritech.contable.maestros.proyecto.presupuesto.dto.PresupuestoProyectoDtos.GuardarRequest;
import com.montanaritech.contable.maestros.proyecto.presupuesto.dto.PresupuestoProyectoDtos.LineaCostoRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Presupuesto por proyecto (F2.6): editable en vivo, sin ciclo de estados —
 * cada consulta recalcula {@link PresupuestoCalculado} a partir de los
 * inputs persistidos + {@link ConfiguracionPresupuesto} vigente, nunca se
 * guarda un resultado derivado.
 */
@Service
@RequiredArgsConstructor
public class PresupuestoProyectoService {

    private final PresupuestoProyectoRepository repo;
    private final ProyectoRepository proyectoRepo;
    private final ConfiguracionPresupuestoRepository configRepo;

    @Transactional(readOnly = true)
    public Optional<PresupuestoProyecto> obtener(Long proyectoId) {
        return repo.findByProyectoId(proyectoId);
    }

    @Transactional
    public PresupuestoProyecto guardar(Long proyectoId, GuardarRequest req) {
        Proyecto proyecto = proyectoRepo.findById(proyectoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto " + proyectoId + " no encontrado"));
        PresupuestoProyecto presupuesto = repo.findByProyectoId(proyectoId).orElseGet(() -> {
            PresupuestoProyecto nuevo = new PresupuestoProyecto();
            nuevo.setProyecto(proyecto);
            return nuevo;
        });

        presupuesto.setMargenDeseadoUsd(req.margenDeseadoUsd());
        presupuesto.setComisionesBancariasIntermediasComexUsd(req.comisionesBancariasIntermediasComexUsd());
        presupuesto.setObservaciones(req.observaciones());

        presupuesto.getLineasCosto().clear();
        List<LineaCostoRequest> lineas = req.lineasCosto() != null ? req.lineasCosto() : List.of();
        for (int i = 0; i < lineas.size(); i++) {
            LineaCostoRequest lineaReq = lineas.get(i);
            PresupuestoLineaCosto linea = new PresupuestoLineaCosto();
            linea.setPresupuestoProyecto(presupuesto);
            linea.setNombre(lineaReq.nombre());
            linea.setImporteUsd(lineaReq.importeUsd());
            linea.setOrden(i);
            presupuesto.getLineasCosto().add(linea);
        }

        return repo.save(presupuesto);
    }

    @Transactional(readOnly = true)
    public PresupuestoCalculado calcular(PresupuestoProyecto presupuesto) {
        ConfiguracionPresupuesto config = configRepo.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("Falta la configuración de presupuesto del sistema"));
        BigDecimal totalCostoProduccion = presupuesto.getLineasCosto().stream()
                .map(PresupuestoLineaCosto::getImporteUsd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Integer cantidadPagosPactados = presupuesto.getProyecto().getCantidadPagosPactados();
        int cantidadPagos = cantidadPagosPactados != null && cantidadPagosPactados > 0 ? cantidadPagosPactados : 1;

        return CalculoPresupuestoProyecto.calcular(
                presupuesto.getProyecto().getTipoProyecto(),
                totalCostoProduccion,
                presupuesto.getMargenDeseadoUsd(),
                presupuesto.getComisionesBancariasIntermediasComexUsd(),
                cantidadPagos,
                config);
    }
}
