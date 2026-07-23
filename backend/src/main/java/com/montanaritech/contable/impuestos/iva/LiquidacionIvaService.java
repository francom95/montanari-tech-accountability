package com.montanaritech.contable.impuestos.iva;

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
import com.montanaritech.contable.impuestos.iva.dto.LiquidacionIvaDtos.AgregarComponenteRequest;
import com.montanaritech.contable.impuestos.iva.dto.LiquidacionIvaDtos.AjustarComponenteRequest;
import com.montanaritech.contable.impuestos.iva.dto.LiquidacionIvaDtos.CrearRequest;
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
 * Ciclo de vida de la liquidación mensual de IVA (F6.1 §1.5/§1.7). Los importes
 * automáticos salen de {@link CalculoIvaService}; acá vive lo que la persona
 * decide encima: ajustar un componente, agregar conceptos, confirmar o anular.
 */
@Service
@RequiredArgsConstructor
public class LiquidacionIvaService {

    private static final Set<EstadoDocumento> ESTADOS_VIVOS =
            Set.of(EstadoDocumento.BORRADOR, EstadoDocumento.CONFIRMADO);

    private final LiquidacionIvaRepository repo;
    private final CalculoIvaService calculoIvaService;
    private final CuentaContableRepository cuentaContableRepository;
    private final LiquidacionIvaAsientoGenerator asientoGenerator;
    private final AsientoService asientoService;
    private final LiquidacionIvaMapper mapper;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public Page<LiquidacionIva> listar(Integer anio, EstadoDocumento estado, Pageable p) {
        return repo.buscar(anio, estado, p);
    }

    @Transactional(readOnly = true)
    public LiquidacionIva obtener(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Liquidación de IVA " + id + " no encontrada"));
    }

    @Transactional(readOnly = true)
    public CalculoIva previsualizar(int anio, int mes) {
        validarPeriodo(anio, mes);
        return calculoIvaService.calcular(anio, mes);
    }

    @Transactional
    public LiquidacionIva crearBorrador(CrearRequest req) {
        validarPeriodo(req.anio(), req.mes());
        if (!repo.findByAnioAndMesAndEstadoIn(req.anio(), req.mes(), ESTADOS_VIVOS).isEmpty()) {
            throw new NegocioException("LIQUIDACION_IVA_YA_EXISTE",
                    "Ya existe una liquidación de IVA de %02d/%d en borrador o confirmada — anulala antes de rehacerla"
                            .formatted(req.mes(), req.anio()));
        }

        CalculoIva calculo = calculoIvaService.calcular(req.anio(), req.mes());
        LiquidacionIva l = new LiquidacionIva();
        l.setAnio(req.anio());
        l.setMes(req.mes());
        l.setFechaDesde(calculo.fechaDesde());
        l.setFechaHasta(calculo.fechaHasta());
        l.setEstado(EstadoDocumento.BORRADOR);

        int orden = 1;
        for (CalculoIva.ComponenteCalculado c : calculo.componentes()) {
            LiquidacionIvaComponente comp = new LiquidacionIvaComponente();
            comp.setLiquidacionIva(l);
            comp.setTipo(c.tipo());
            comp.setDescripcion(c.descripcion());
            comp.setImporteCalculado(escalar(c.importe()));
            comp.setImporteAjuste(BigDecimal.ZERO);
            comp.setManual(false);
            comp.setOrden(orden++);
            l.getComponentes().add(comp);
        }

        recalcularResultado(l);
        LiquidacionIva guardada = repo.save(l);
        auditoria.registrar(AccionAuditoria.CREAR, "LiquidacionIva", guardada.getId(), null, mapper.aResponse(guardada, List.of()));
        return guardada;
    }

    /**
     * Vuelve a calcular los componentes automáticos conservando los ajustes
     * manuales y los conceptos agregados a mano. Sirve cuando se confirmaron
     * facturas del período después de haber abierto el borrador.
     */
    @Transactional
    public LiquidacionIva recalcular(Long id) {
        LiquidacionIva l = obtener(id);
        exigirBorrador(l, "recalcular");
        var antes = mapper.aResponse(l, List.of());

        CalculoIva calculo = calculoIvaService.calcular(l.getAnio(), l.getMes());
        for (CalculoIva.ComponenteCalculado c : calculo.componentes()) {
            l.getComponentes().stream()
                    .filter(comp -> !comp.isManual() && comp.getTipo() == c.tipo())
                    .findFirst()
                    .ifPresent(comp -> {
                        comp.setImporteCalculado(escalar(c.importe()));
                        comp.setDescripcion(c.descripcion());
                    });
        }

        recalcularResultado(l);
        auditoria.registrar(AccionAuditoria.EDITAR, "LiquidacionIva", id, antes, mapper.aResponse(l, calculo.advertencias()));
        return l;
    }

    @Transactional
    public LiquidacionIva ajustarComponente(Long id, Long componenteId, AjustarComponenteRequest req) {
        LiquidacionIva l = obtener(id);
        exigirBorrador(l, "ajustar");
        var antes = mapper.aResponse(l, List.of());

        LiquidacionIvaComponente comp = l.getComponentes().stream()
                .filter(c -> c.getId().equals(componenteId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El componente " + componenteId + " no pertenece a esta liquidación"));

        BigDecimal ajuste = escalar(req.importeAjuste());
        if (ajuste.signum() != 0 && (req.motivoAjuste() == null || req.motivoAjuste().isBlank())) {
            throw new NegocioException("AJUSTE_IVA_SIN_MOTIVO",
                    "Un ajuste manual sobre \"%s\" necesita un motivo — queda registrado en la auditoría"
                            .formatted(comp.getDescripcion()));
        }

        comp.setImporteAjuste(ajuste);
        comp.setMotivoAjuste(ajuste.signum() == 0 ? null : req.motivoAjuste());
        recalcularResultado(l);

        auditoria.registrar(AccionAuditoria.EDITAR, "LiquidacionIva", id, antes, mapper.aResponse(l, List.of()));
        return l;
    }

    @Transactional
    public LiquidacionIva agregarComponente(Long id, AgregarComponenteRequest req) {
        LiquidacionIva l = obtener(id);
        exigirBorrador(l, "agregar conceptos a");
        var antes = mapper.aResponse(l, List.of());

        if (req.tipo().esAutomatico()) {
            throw new NegocioException("COMPONENTE_IVA_NO_MANUAL",
                    ("%s se calcula automáticamente desde los asientos del período: ajustá el componente que ya está "
                            + "en la liquidación en vez de agregar uno nuevo").formatted(req.tipo()));
        }
        CuentaContable cuenta = cuentaContableRepository.findById(req.cuentaContableId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta contable " + req.cuentaContableId() + " no encontrada"));

        LiquidacionIvaComponente comp = new LiquidacionIvaComponente();
        comp.setLiquidacionIva(l);
        comp.setTipo(req.tipo());
        comp.setDescripcion(req.descripcion());
        comp.setImporteCalculado(escalar(req.importe()));
        comp.setImporteAjuste(BigDecimal.ZERO);
        comp.setMotivoAjuste(req.motivo());
        comp.setCuentaContable(cuenta);
        comp.setManual(true);
        comp.setOrden(l.getComponentes().stream().mapToInt(LiquidacionIvaComponente::getOrden).max().orElse(0) + 1);
        l.getComponentes().add(comp);

        recalcularResultado(l);
        auditoria.registrar(AccionAuditoria.EDITAR, "LiquidacionIva", id, antes, mapper.aResponse(l, List.of()));
        return l;
    }

    @Transactional
    public LiquidacionIva eliminarComponente(Long id, Long componenteId) {
        LiquidacionIva l = obtener(id);
        exigirBorrador(l, "modificar");
        var antes = mapper.aResponse(l, List.of());

        LiquidacionIvaComponente comp = l.getComponentes().stream()
                .filter(c -> c.getId().equals(componenteId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El componente " + componenteId + " no pertenece a esta liquidación"));
        if (!comp.isManual()) {
            throw new NegocioException("COMPONENTE_IVA_NO_ELIMINABLE",
                    ("\"%s\" se calcula automáticamente y no se puede eliminar; si querés dejarlo en cero, "
                            + "ajustalo con el motivo correspondiente").formatted(comp.getDescripcion()));
        }

        l.getComponentes().remove(comp);
        recalcularResultado(l);
        auditoria.registrar(AccionAuditoria.EDITAR, "LiquidacionIva", id, antes, mapper.aResponse(l, List.of()));
        return l;
    }

    @Transactional
    public LiquidacionIva confirmar(Long id) {
        LiquidacionIva l = obtener(id);
        var antes = mapper.aResponse(l, List.of());
        TransicionEstadoValidator.validar(l.getEstado(), EstadoDocumento.CONFIRMADO);

        Asiento asiento = asientoService.registrarAutomatico(asientoGenerator.generar(l));
        l.setAsiento(asiento);
        l.setEstado(EstadoDocumento.CONFIRMADO);

        auditoria.registrar(AccionAuditoria.CONFIRMAR, "LiquidacionIva", id, antes, mapper.aResponse(l, List.of()));
        return l;
    }

    /**
     * "Des-confirmar" del plan: anula el asiento (regla F3.1, marca + motivo,
     * conserva trazabilidad) y libera el período. No vuelve a BORRADOR porque
     * PL-5 prohíbe las transiciones hacia atrás — rehacer la liquidación es
     * crear otra del mismo período.
     */
    @Transactional
    public LiquidacionIva anular(Long id, String motivo) {
        LiquidacionIva l = obtener(id);
        var antes = mapper.aResponse(l, List.of());
        TransicionEstadoValidator.validar(l.getEstado(), EstadoDocumento.ANULADO);

        if (l.getAsiento() != null) {
            asientoService.anularPorDocumento(l.getAsiento().getId(), motivo);
        }
        l.setEstado(EstadoDocumento.ANULADO);
        l.setObservaciones(motivo);

        auditoria.registrar(AccionAuditoria.ANULAR, "LiquidacionIva", id, antes, mapper.aResponse(l, List.of()));
        return l;
    }

    /**
     * Resultado en sus dos caras excluyentes (F6.1 §1.3): la suma de los aportes
     * con signo cae de un lado o del otro, nunca de los dos.
     */
    private void recalcularResultado(LiquidacionIva l) {
        BigDecimal resultado = l.getComponentes().stream()
                .map(LiquidacionIvaComponente::getAporte)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        resultado = escalar(resultado);

        if (resultado.signum() > 0) {
            l.setSaldoAPagar(resultado);
            l.setSaldoAFavor(BigDecimal.ZERO.setScale(2));
        } else {
            l.setSaldoAPagar(BigDecimal.ZERO.setScale(2));
            l.setSaldoAFavor(resultado.negate());
        }
    }

    private void exigirBorrador(LiquidacionIva l, String accion) {
        if (l.getEstado() != EstadoDocumento.BORRADOR) {
            throw new NegocioException("LIQUIDACION_IVA_NO_EDITABLE",
                    "Solo se puede %s una liquidación en borrador (esta está %s)".formatted(accion, l.getEstado()));
        }
    }

    private void validarPeriodo(int anio, int mes) {
        if (mes < 1 || mes > 12) {
            throw new NegocioException("PERIODO_INVALIDO", "El mes debe estar entre 1 y 12");
        }
        YearMonth.of(anio, mes); // valida el año por rango del propio tipo
    }

    /** Scale fijo en 2: evita que un {@code BigDecimal} sin escalar salga en notación científica al serializar. */
    private BigDecimal escalar(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
