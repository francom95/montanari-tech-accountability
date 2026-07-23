package com.montanaritech.contable.impuestos.iibb;

import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.common.estado.TransicionEstadoValidator;
import com.montanaritech.contable.contabilidad.asiento.Asiento;
import com.montanaritech.contable.contabilidad.asiento.AsientoService;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContableRepository;
import com.montanaritech.contable.impuestos.iibb.dto.LiquidacionIibbDtos.AgregarComponenteRequest;
import com.montanaritech.contable.impuestos.iibb.dto.LiquidacionIibbDtos.AjustarComponenteRequest;
import com.montanaritech.contable.impuestos.iibb.dto.LiquidacionIibbDtos.CrearRequest;
import com.montanaritech.contable.impuestos.iibb.dto.LiquidacionIibbDtos.EditarJurisdiccionRequest;
import com.montanaritech.contable.maestros.jurisdiccion.Jurisdiccion;
import com.montanaritech.contable.maestros.jurisdiccion.JurisdiccionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ciclo de vida de la liquidación mensual de IIBB (F6.2). Reusa la arquitectura
 * de F6.1 (PL-5, componentes con ajuste auditado, previsualización, des-confirmar
 * = anular) pero determina el impuesto <b>por jurisdicción</b>.
 */
@Service
@RequiredArgsConstructor
public class LiquidacionIibbService {

    private static final Set<EstadoDocumento> ESTADOS_VIVOS =
            Set.of(EstadoDocumento.BORRADOR, EstadoDocumento.CONFIRMADO);

    /** Deducciones que se precrean en cero por jurisdicción para que el usuario cargue el importe. */
    private static final List<TipoComponenteIibb> DEDUCCIONES = List.of(
            TipoComponenteIibb.PERCEPCIONES, TipoComponenteIibb.RETENCIONES,
            TipoComponenteIibb.SIRCREB, TipoComponenteIibb.PAGOS_A_CUENTA);

    private final LiquidacionIibbRepository repo;
    private final CalculoIibbService calculoIibbService;
    private final JurisdiccionRepository jurisdiccionRepository;
    private final CuentaContableRepository cuentaContableRepository;
    private final LiquidacionIibbAsientoGenerator asientoGenerator;
    private final AsientoService asientoService;
    private final LiquidacionIibbMapper mapper;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public Page<LiquidacionIibb> listar(Integer anio, EstadoDocumento estado, Pageable p) {
        return repo.buscar(anio, estado, p);
    }

    @Transactional(readOnly = true)
    public LiquidacionIibb obtener(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Liquidación de IIBB " + id + " no encontrada"));
    }

    @Transactional(readOnly = true)
    public CalculoIibb previsualizar(int anio, int mes) {
        validarPeriodo(anio, mes);
        return calculoIibbService.calcular(anio, mes);
    }

    @Transactional
    public LiquidacionIibb crearBorrador(CrearRequest req) {
        validarPeriodo(req.anio(), req.mes());
        if (!repo.findByAnioAndMesAndEstadoIn(req.anio(), req.mes(), ESTADOS_VIVOS).isEmpty()) {
            throw new NegocioException("LIQUIDACION_IIBB_YA_EXISTE",
                    "Ya existe una liquidación de IIBB de %02d/%d en borrador o confirmada — anulala antes de rehacerla"
                            .formatted(req.mes(), req.anio()));
        }

        CalculoIibb calculo = calculoIibbService.calcular(req.anio(), req.mes());
        LiquidacionIibb l = new LiquidacionIibb();
        l.setAnio(req.anio());
        l.setMes(req.mes());
        l.setFechaDesde(calculo.fechaDesde());
        l.setFechaHasta(calculo.fechaHasta());
        l.setEstado(EstadoDocumento.BORRADOR);
        l.setBaseTotal(calculo.baseTotal());

        // Traer de contabilidad (F6.2 ajuste 2): si hay una sola jurisdicción con base,
        // las percepciones/SIRCREB registradas en 1.1.2008 se le precargan; con varias,
        // quedan en cero para repartir a mano (la advertencia del cálculo lo avisa).
        long conBase = calculo.jurisdicciones().stream().filter(jc -> jc.baseImponible().signum() != 0).count();
        Long jurUnicaConBase = conBase == 1
                ? calculo.jurisdicciones().stream().filter(jc -> jc.baseImponible().signum() != 0)
                        .findFirst().get().jurisdiccionId()
                : null;

        int ordenJur = 1;
        for (CalculoIibb.JurisdiccionCalculada jc : calculo.jurisdicciones()) {
            Jurisdiccion jur = jurisdiccionRepository.findById(jc.jurisdiccionId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Jurisdicción " + jc.jurisdiccionId() + " no encontrada"));
            LiquidacionIibbJurisdiccion lj = new LiquidacionIibbJurisdiccion();
            lj.setLiquidacionIibb(l);
            lj.setJurisdiccion(jur);
            lj.setCoeficiente(jc.coeficiente());
            lj.setBaseImponible(jc.baseImponible());
            lj.setAlicuota(jc.alicuota());
            lj.setImpuestoDeterminado(jc.impuestoDeterminado());
            lj.setOrden(ordenJur++);

            int ordenComp = 1;
            for (TipoComponenteIibb tipo : DEDUCCIONES) {
                BigDecimal calculado = tipo == TipoComponenteIibb.SIRCREB && jc.jurisdiccionId().equals(jurUnicaConBase)
                        ? calculo.deduccionesDisponibles()
                        : BigDecimal.ZERO;
                lj.getComponentes().add(componente(lj, tipo, calculado, false, ordenComp++));
            }
            if (jc.saldoAFavorAnterior().signum() != 0) {
                lj.getComponentes().add(componente(lj, TipoComponenteIibb.SALDO_A_FAVOR_ANTERIOR,
                        jc.saldoAFavorAnterior(), false, ordenComp++));
            }

            recalcularJurisdiccion(lj);
            l.getJurisdicciones().add(lj);
        }

        recalcularTotales(l);
        LiquidacionIibb guardada = repo.save(l);
        auditoria.registrar(AccionAuditoria.CREAR, "LiquidacionIibb", guardada.getId(), null,
                mapper.aResponse(guardada, List.of()));
        return guardada;
    }

    /**
     * Refresca la base del período (por si se confirmaron facturas después de abrir
     * el borrador) y el arrastre, conservando el coeficiente y la alícuota que el
     * usuario haya editado y las deducciones cargadas a mano.
     */
    @Transactional
    public LiquidacionIibb recalcular(Long id) {
        LiquidacionIibb l = obtener(id);
        exigirBorrador(l, "recalcular");
        var antes = mapper.aResponse(l, List.of());

        CalculoIibb calculo = calculoIibbService.calcular(l.getAnio(), l.getMes());
        l.setBaseTotal(calculo.baseTotal());
        for (LiquidacionIibbJurisdiccion lj : l.getJurisdicciones()) {
            recomputarDeterminacion(lj);
            calculo.jurisdicciones().stream()
                    .filter(jc -> jc.jurisdiccionId().equals(lj.getJurisdiccion().getId()))
                    .findFirst()
                    .ifPresent(jc -> actualizarArrastre(lj, jc.saldoAFavorAnterior()));
            recalcularJurisdiccion(lj);
        }
        recalcularTotales(l);

        auditoria.registrar(AccionAuditoria.EDITAR, "LiquidacionIibb", id, antes,
                mapper.aResponse(l, calculo.advertencias()));
        return l;
    }

    @Transactional
    public LiquidacionIibb editarJurisdiccion(Long id, Long jurLiqId, EditarJurisdiccionRequest req) {
        LiquidacionIibb l = obtener(id);
        exigirBorrador(l, "editar");
        var antes = mapper.aResponse(l, List.of());

        LiquidacionIibbJurisdiccion lj = jurisdiccionDe(l, jurLiqId);
        lj.setCoeficiente(req.coeficiente().setScale(6, RoundingMode.HALF_UP));
        lj.setAlicuota(req.alicuota().setScale(2, RoundingMode.HALF_UP));
        recomputarDeterminacion(lj);
        recalcularJurisdiccion(lj);
        recalcularTotales(l);

        auditoria.registrar(AccionAuditoria.EDITAR, "LiquidacionIibb", id, antes, mapper.aResponse(l, List.of()));
        return l;
    }

    @Transactional
    public LiquidacionIibb ajustarComponente(Long id, Long jurLiqId, Long componenteId, AjustarComponenteRequest req) {
        LiquidacionIibb l = obtener(id);
        exigirBorrador(l, "ajustar");
        var antes = mapper.aResponse(l, List.of());

        LiquidacionIibbJurisdiccion lj = jurisdiccionDe(l, jurLiqId);
        LiquidacionIibbComponente comp = lj.getComponentes().stream()
                .filter(c -> c.getId().equals(componenteId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El componente " + componenteId + " no pertenece a esta jurisdicción"));

        BigDecimal ajuste = escalar(req.importeAjuste());
        // El motivo solo es obligatorio si se corrige un valor que el sistema calculó
        // (el arrastre). Cargar una deducción que el sistema no podía conocer es
        // ingreso de datos, no una corrección, y no necesita motivo (F6.2 §1.3).
        boolean corrigeValorCalculado = comp.getImporteCalculado().signum() != 0 && ajuste.signum() != 0;
        if (corrigeValorCalculado && (req.motivoAjuste() == null || req.motivoAjuste().isBlank())) {
            throw new NegocioException("AJUSTE_IIBB_SIN_MOTIVO",
                    "Corregir \"%s\" (un valor calculado por el sistema) necesita un motivo".formatted(comp.getDescripcion()));
        }

        comp.setImporteAjuste(ajuste);
        comp.setMotivoAjuste(req.motivoAjuste() == null || req.motivoAjuste().isBlank() ? null : req.motivoAjuste());
        recalcularJurisdiccion(lj);
        recalcularTotales(l);

        auditoria.registrar(AccionAuditoria.EDITAR, "LiquidacionIibb", id, antes, mapper.aResponse(l, List.of()));
        return l;
    }

    @Transactional
    public LiquidacionIibb agregarComponente(Long id, Long jurLiqId, AgregarComponenteRequest req) {
        LiquidacionIibb l = obtener(id);
        exigirBorrador(l, "agregar conceptos a");
        var antes = mapper.aResponse(l, List.of());

        if (req.tipo() != TipoComponenteIibb.OTRO) {
            throw new NegocioException("COMPONENTE_IIBB_NO_MANUAL",
                    "Solo se pueden agregar componentes de tipo OTRO; las deducciones ya están en la liquidación para cargar su importe");
        }
        CuentaContable cuenta = cuentaContableRepository.findById(req.cuentaContableId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta contable " + req.cuentaContableId() + " no encontrada"));

        LiquidacionIibbJurisdiccion lj = jurisdiccionDe(l, jurLiqId);
        LiquidacionIibbComponente comp = componente(lj, req.tipo(), escalar(req.importe()), true,
                lj.getComponentes().stream().mapToInt(LiquidacionIibbComponente::getOrden).max().orElse(0) + 1);
        comp.setDescripcion(req.descripcion());
        comp.setCuentaContable(cuenta);
        comp.setMotivoAjuste(req.motivo());
        lj.getComponentes().add(comp);

        recalcularJurisdiccion(lj);
        recalcularTotales(l);
        auditoria.registrar(AccionAuditoria.EDITAR, "LiquidacionIibb", id, antes, mapper.aResponse(l, List.of()));
        return l;
    }

    @Transactional
    public LiquidacionIibb eliminarComponente(Long id, Long jurLiqId, Long componenteId) {
        LiquidacionIibb l = obtener(id);
        exigirBorrador(l, "modificar");
        var antes = mapper.aResponse(l, List.of());

        LiquidacionIibbJurisdiccion lj = jurisdiccionDe(l, jurLiqId);
        LiquidacionIibbComponente comp = lj.getComponentes().stream()
                .filter(c -> c.getId().equals(componenteId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El componente " + componenteId + " no pertenece a esta jurisdicción"));
        if (!comp.isManual()) {
            throw new NegocioException("COMPONENTE_IIBB_NO_ELIMINABLE",
                    "\"%s\" es un concepto fijo de la liquidación; si querés dejarlo en cero, ajustá su importe a cero"
                            .formatted(comp.getDescripcion()));
        }
        lj.getComponentes().remove(comp);
        recalcularJurisdiccion(lj);
        recalcularTotales(l);
        auditoria.registrar(AccionAuditoria.EDITAR, "LiquidacionIibb", id, antes, mapper.aResponse(l, List.of()));
        return l;
    }

    @Transactional
    public LiquidacionIibb confirmar(Long id) {
        LiquidacionIibb l = obtener(id);
        var antes = mapper.aResponse(l, List.of());
        TransicionEstadoValidator.validar(l.getEstado(), EstadoDocumento.CONFIRMADO);

        Asiento asiento = asientoService.registrarAutomatico(asientoGenerator.generar(l));
        l.setAsiento(asiento);
        l.setEstado(EstadoDocumento.CONFIRMADO);

        auditoria.registrar(AccionAuditoria.CONFIRMAR, "LiquidacionIibb", id, antes, mapper.aResponse(l, List.of()));
        return l;
    }

    @Transactional
    public LiquidacionIibb anular(Long id, String motivo) {
        LiquidacionIibb l = obtener(id);
        var antes = mapper.aResponse(l, List.of());
        TransicionEstadoValidator.validar(l.getEstado(), EstadoDocumento.ANULADO);

        if (l.getAsiento() != null) {
            asientoService.anularPorDocumento(l.getAsiento().getId(), motivo);
        }
        l.setEstado(EstadoDocumento.ANULADO);
        l.setObservaciones(motivo);

        auditoria.registrar(AccionAuditoria.ANULAR, "LiquidacionIibb", id, antes, mapper.aResponse(l, List.of()));
        return l;
    }

    // --- helpers ---

    private LiquidacionIibbComponente componente(LiquidacionIibbJurisdiccion lj, TipoComponenteIibb tipo,
                                                 BigDecimal calculado, boolean manual, int orden) {
        LiquidacionIibbComponente c = new LiquidacionIibbComponente();
        c.setLiquidacionIibbJurisdiccion(lj);
        c.setTipo(tipo);
        c.setDescripcion(tipo.getDescripcionPorDefecto());
        c.setImporteCalculado(calculado);
        c.setImporteAjuste(BigDecimal.ZERO);
        c.setManual(manual);
        c.setOrden(orden);
        return c;
    }

    /** baseImponible = baseTotal × coeficiente; impuestoDeterminado = baseImponible × alícuota. */
    private void recomputarDeterminacion(LiquidacionIibbJurisdiccion lj) {
        BigDecimal base = escalar(lj.getLiquidacionIibb().getBaseTotal().multiply(lj.getCoeficiente()));
        lj.setBaseImponible(base);
        lj.setImpuestoDeterminado(escalar(base.multiply(lj.getAlicuota()).divide(BigDecimal.valueOf(100))));
    }

    private void actualizarArrastre(LiquidacionIibbJurisdiccion lj, BigDecimal arrastre) {
        lj.getComponentes().stream()
                .filter(c -> c.getTipo() == TipoComponenteIibb.SALDO_A_FAVOR_ANTERIOR)
                .findFirst()
                .ifPresent(c -> c.setImporteCalculado(escalar(arrastre)));
    }

    /** Una sola etapa: neto = determinado + Σ aportes (deducciones restan). */
    private void recalcularJurisdiccion(LiquidacionIibbJurisdiccion lj) {
        BigDecimal neto = lj.getImpuestoDeterminado();
        for (LiquidacionIibbComponente c : lj.getComponentes()) {
            neto = neto.add(c.getAporte());
        }
        neto = escalar(neto);
        lj.setSaldoAPagar(neto.max(BigDecimal.ZERO));
        lj.setSaldoAFavor(neto.min(BigDecimal.ZERO).negate());
    }

    private void recalcularTotales(LiquidacionIibb l) {
        BigDecimal aPagar = BigDecimal.ZERO;
        BigDecimal aFavor = BigDecimal.ZERO;
        for (LiquidacionIibbJurisdiccion lj : l.getJurisdicciones()) {
            aPagar = aPagar.add(lj.getSaldoAPagar());
            aFavor = aFavor.add(lj.getSaldoAFavor());
        }
        l.setSaldoAPagarTotal(escalar(aPagar));
        l.setSaldoAFavorTotal(escalar(aFavor));
    }

    private LiquidacionIibbJurisdiccion jurisdiccionDe(LiquidacionIibb l, Long jurLiqId) {
        return l.getJurisdicciones().stream()
                .filter(j -> j.getId().equals(jurLiqId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La jurisdicción " + jurLiqId + " no pertenece a esta liquidación"));
    }

    private void exigirBorrador(LiquidacionIibb l, String accion) {
        if (l.getEstado() != EstadoDocumento.BORRADOR) {
            throw new NegocioException("LIQUIDACION_IIBB_NO_EDITABLE",
                    "Solo se puede %s una liquidación en borrador (esta está %s)".formatted(accion, l.getEstado()));
        }
    }

    private void validarPeriodo(int anio, int mes) {
        if (mes < 1 || mes > 12) {
            throw new NegocioException("PERIODO_INVALIDO", "El mes debe estar entre 1 y 12");
        }
        YearMonth.of(anio, mes);
    }

    private BigDecimal escalar(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
