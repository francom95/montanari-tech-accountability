package com.montanaritech.contable.periodo;

import com.montanaritech.contable.bancos.conciliacion.ConciliacionService;
import com.montanaritech.contable.bancos.conciliacion.dto.ConciliacionResumenResponse;
import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.auth.RolActualUtil;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.error.PeriodoCerradoException;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.contabilidad.asiento.AsientoRepository;
import com.montanaritech.contable.impuestos.iibb.LiquidacionIibbRepository;
import com.montanaritech.contable.impuestos.iva.LiquidacionIvaRepository;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancariaRepository;
import com.montanaritech.contable.periodo.dto.PeriodoDtos.LiquidacionResumenItem;
import com.montanaritech.contable.periodo.dto.PeriodoDtos.PeriodoResumenResponse;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Períodos contables mensuales (F9.3): el cierre es un control, no una
 * traba por defecto — consultar/importar/exportar nunca se bloquean, solo
 * crear/editar/anular con fecha dentro de un período cerrado (ver
 * {@link #verificarEscritura}, invocado desde {@code AsientoService} y los
 * 4 servicios de documento de F4.x).
 */
@Service
@RequiredArgsConstructor
public class PeriodoService {

    /** Misma tolerancia usada por {@code MotorAlertasService} (F9.1) para el resumen de conciliación por cuenta. */
    private static final int TOLERANCIA_DIAS_CONCILIACION = 3;

    private final PeriodoRepository repo;
    private final PeriodoMapper mapper;
    private final AsientoRepository asientoRepo;
    private final LiquidacionIvaRepository liquidacionIvaRepo;
    private final LiquidacionIibbRepository liquidacionIibbRepo;
    private final CuentaBancariaRepository cuentaBancariaRepo;
    private final ConciliacionService conciliacionService;
    private final AuditoriaService auditoria;

    /** Ausencia de fila o estado ≠ CERRADO ⇒ abierto (implícito o EN_REVISION, que se comporta igual por decisión del usuario). */
    @Transactional(readOnly = true)
    public boolean estaCerrado(LocalDate fecha) {
        return repo.findByAnioAndMes(fecha.getYear(), fecha.getMonthValue())
                .map(p -> p.getEstado() == EstadoPeriodo.CERRADO)
                .orElse(false);
    }

    /**
     * Gate único de escritura (F9.3): no-op (retorna {@code false}) si el
     * período está abierto. Si está cerrado, exige rol admin y, además,
     * confirmación explícita + motivo; si todo eso se cumple, retorna
     * {@code true} — el caller usa ese valor para auditar con
     * {@code sobrePeriodoCerrado=true, detalle=motivoOverridePeriodo}.
     */
    @Transactional(readOnly = true)
    public boolean verificarEscritura(LocalDate fecha, boolean confirmarPeriodoCerrado, String motivoOverridePeriodo) {
        if (!estaCerrado(fecha)) {
            return false;
        }
        if (!RolActualUtil.esAdmin()) {
            throw new PeriodoCerradoException(
                    "El período %02d/%d está cerrado: solo un administrador puede modificarlo".formatted(fecha.getMonthValue(), fecha.getYear()));
        }
        if (!confirmarPeriodoCerrado || motivoOverridePeriodo == null || motivoOverridePeriodo.isBlank()) {
            throw new PeriodoCerradoException(
                    "El período %02d/%d está cerrado: confirmá la acción con un motivo para continuar".formatted(fecha.getMonthValue(), fecha.getYear()));
        }
        return true;
    }

    @Transactional(readOnly = true)
    public Page<Periodo> listar(EstadoPeriodo estado, Pageable p) {
        return repo.buscar(estado, p);
    }

    @Transactional(readOnly = true)
    public Periodo obtener(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Período " + id + " no encontrado"));
    }

    /**
     * Motor on-demand (F9.3, molde de {@code VencimientoService.generarAutomaticos()}
     * de F8.1): idempotente, un {@code ABIERTO} por cada (año, mes) con al
     * menos un asiento que no tenga fila todavía.
     */
    @Transactional
    public int generarAutomaticos() {
        int generados = 0;
        for (Object[] fila : asientoRepo.findDistinctAnioMesConAsientos()) {
            int anio = ((Number) fila[0]).intValue();
            int mes = ((Number) fila[1]).intValue();
            if (repo.existsByAnioAndMes(anio, mes)) {
                continue;
            }
            Periodo p = new Periodo();
            p.setAnio(anio);
            p.setMes(mes);
            p.setEstado(EstadoPeriodo.ABIERTO);
            repo.save(p);
            generados++;
        }
        return generados;
    }

    @Transactional
    public Periodo cerrar(Long id, String motivo) {
        Periodo p = obtener(id);
        var antes = mapper.aResponse(p);
        p.setEstado(EstadoPeriodo.CERRADO);
        p.setMotivoCierre(motivo);
        auditoria.registrar(AccionAuditoria.CERRAR_PERIODO, "Periodo", id, antes, mapper.aResponse(p));
        return p;
    }

    @Transactional
    public Periodo marcarEnRevision(Long id) {
        Periodo p = obtener(id);
        var antes = mapper.aResponse(p);
        p.setEstado(EstadoPeriodo.EN_REVISION);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "Periodo", id, antes, mapper.aResponse(p));
        return p;
    }

    @Transactional
    public Periodo reabrir(Long id, String motivo) {
        Periodo p = obtener(id);
        if (p.getEstado() != EstadoPeriodo.CERRADO) {
            throw new NegocioException("PERIODO_NO_CERRADO", "Solo se puede reabrir un período cerrado");
        }
        var antes = mapper.aResponse(p);
        p.setEstado(EstadoPeriodo.ABIERTO);
        p.setMotivoReapertura(motivo);
        auditoria.registrar(AccionAuditoria.REABRIR_PERIODO, "Periodo", id, antes, mapper.aResponse(p));
        return p;
    }

    /**
     * Resumen para la pantalla admin (F9.3): liquidaciones de IVA/IIBB del
     * mes + conciliación de cada cuenta bancaria activa en el rango del
     * período. No hay entidad {@code Conciliacion} persistida (F5.3 es
     * 100% query en vivo), así que se recalcula acá con
     * {@link ConciliacionService#resumen}.
     */
    @Transactional(readOnly = true)
    public PeriodoResumenResponse resumen(Long id) {
        Periodo p = obtener(id);
        LocalDate desde = LocalDate.of(p.getAnio(), p.getMes(), 1);
        LocalDate hasta = YearMonth.of(p.getAnio(), p.getMes()).atEndOfMonth();

        List<LiquidacionResumenItem> liquidaciones = new ArrayList<>();
        liquidacionIvaRepo.findByAnioAndMes(p.getAnio(), p.getMes()).forEach(l ->
                liquidaciones.add(new LiquidacionResumenItem("IVA", l.getId(), l.getEstado().name(), l.getSaldoAPagar())));
        liquidacionIibbRepo.findByAnioAndMes(p.getAnio(), p.getMes()).forEach(l ->
                liquidaciones.add(new LiquidacionResumenItem("IIBB", l.getId(), l.getEstado().name(), l.getSaldoAPagarTotal())));

        List<ConciliacionResumenResponse> conciliaciones = new ArrayList<>();
        for (var cuenta : cuentaBancariaRepo.findByActivoTrue()) {
            conciliaciones.add(conciliacionService.resumen(cuenta.getId(), desde, hasta, TOLERANCIA_DIAS_CONCILIACION));
        }

        return new PeriodoResumenResponse(p.getId(), p.getAnio(), p.getMes(), p.getEstado(), liquidaciones, conciliaciones);
    }
}
