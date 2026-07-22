package com.montanaritech.contable.bancos.movimientobancario;

import com.montanaritech.contable.bancos.movimientobancario.dto.CorregirMovimientoBancarioRequest;
import com.montanaritech.contable.bancos.movimientobancario.dto.CrearMovimientoBancarioRequest;
import com.montanaritech.contable.common.asiento.AsientoGenerado;
import com.montanaritech.contable.common.asiento.CalculoImputacion;
import com.montanaritech.contable.common.asiento.LineaAsientoGenerada;
import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.contabilidad.asiento.Asiento;
import com.montanaritech.contable.contabilidad.asiento.AsientoLinea;
import com.montanaritech.contable.contabilidad.asiento.AsientoRepository;
import com.montanaritech.contable.contabilidad.asiento.AsientoService;
import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContableRepository;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancaria;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancariaRepository;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bandeja de movimientos bancarios pendientes de revisar (F5.1). */
@Service
@RequiredArgsConstructor
public class MovimientoBancarioService {

    private static final String MONEDA_LIBRO = "ARS";

    private final MovimientoBancarioRepository repo;
    private final MovimientoBancarioMapper mapper;
    private final AuditoriaService auditoria;
    private final AsientoService asientoService;
    private final AsientoRepository asientoRepo;
    private final CuentaBancariaRepository cuentaBancariaRepo;
    private final MonedaRepository monedaRepo;
    private final CuentaContableRepository cuentaContableRepo;

    @Transactional(readOnly = true)
    public Page<MovimientoBancario> listar(Long cuentaBancariaId, EstadoMovimientoBancario estado,
            LocalDate fechaDesde, LocalDate fechaHasta, Pageable p) {
        return repo.buscar(cuentaBancariaId, estado, fechaDesde, fechaHasta, p);
    }

    @Transactional(readOnly = true)
    public MovimientoBancario obtener(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Movimiento bancario " + id + " no encontrado"));
    }

    @Transactional(readOnly = true)
    public long contarPendientes(Long cuentaBancariaId) {
        return cuentaBancariaId != null
                ? repo.countByEstadoAndCuentaBancaria_Id(EstadoMovimientoBancario.PENDIENTE, cuentaBancariaId)
                : repo.countByEstado(EstadoMovimientoBancario.PENDIENTE);
    }

    @Transactional
    public MovimientoBancario crear(CrearMovimientoBancarioRequest req) {
        MovimientoBancario m = new MovimientoBancario();
        aplicarCampos(m, req.cuentaBancariaId(), req.fecha(), req.descripcion(), req.importe(), req.monedaId(),
                req.tipoCambio(), req.referencia(), req.cuentaContableSugeridaId(), req.observaciones());
        m.setEstado(EstadoMovimientoBancario.PENDIENTE);
        m.setOrigenImportacion(req.origenImportacion() != null ? req.origenImportacion() : OrigenImportacionMovimiento.MANUAL);
        m.setHashImportacion(req.hashImportacion());

        MovimientoBancario guardado = repo.save(m);
        auditoria.registrar(AccionAuditoria.CREAR, "MovimientoBancario", guardado.getId(), null, mapper.aResponse(guardado));
        return guardado;
    }

    @Transactional
    public MovimientoBancario corregir(Long id, CorregirMovimientoBancarioRequest req) {
        MovimientoBancario m = obtenerPendiente(id);
        var antes = mapper.aResponse(m);

        aplicarCampos(m, req.cuentaBancariaId(), req.fecha(), req.descripcion(), req.importe(), req.monedaId(),
                req.tipoCambio(), req.referencia(), req.cuentaContableSugeridaId(), req.observaciones());

        auditoria.registrar(AccionAuditoria.EDITAR, "MovimientoBancario", id, antes, mapper.aResponse(m));
        return m;
    }

    @Transactional
    public MovimientoBancario confirmar(Long id) {
        MovimientoBancario m = obtenerPendiente(id);
        var antes = mapper.aResponse(m);

        if (m.getCuentaContableSugerida() == null) {
            throw new NegocioException("SIN_CUENTA_SUGERIDA",
                    "Este movimiento no tiene una cuenta sugerida — usá \"imputar\" para elegir una cuenta");
        }
        exigirFecha(m);

        Asiento asiento = generarYRegistrarAsiento(m, m.getCuentaContableSugerida());
        m.setAsiento(asiento);
        m.setEstado(EstadoMovimientoBancario.CONCILIADO);

        auditoria.registrar(AccionAuditoria.CONFIRMAR, "MovimientoBancario", id, antes, mapper.aResponse(m));
        return m;
    }

    @Transactional
    public MovimientoBancario imputar(Long id, Long cuentaContableId) {
        MovimientoBancario m = obtenerPendiente(id);
        var antes = mapper.aResponse(m);

        CuentaContable cuenta = cuentaContableRepo.findById(cuentaContableId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta contable " + cuentaContableId + " no encontrada"));
        exigirFecha(m);

        Asiento asiento = generarYRegistrarAsiento(m, cuenta);
        m.setAsiento(asiento);
        m.setEstado(EstadoMovimientoBancario.CONCILIADO);

        auditoria.registrar(AccionAuditoria.CONFIRMAR, "MovimientoBancario", id, antes, mapper.aResponse(m));
        return m;
    }

    @Transactional
    public MovimientoBancario asociar(Long id, Long asientoNumero) {
        MovimientoBancario m = obtenerPendiente(id);
        var antes = mapper.aResponse(m);

        Asiento asiento = asientoRepo.findByNumero(asientoNumero)
                .orElseThrow(() -> new RecursoNoEncontradoException("Asiento N° " + asientoNumero + " no encontrado"));
        if (asiento.getEstado() != EstadoDocumento.CONFIRMADO) {
            throw new NegocioException("ASIENTO_NO_CONFIRMADO", "El asiento N° " + asientoNumero + " no está confirmado");
        }
        if (repo.existsByAsiento_Id(asiento.getId())) {
            throw new NegocioException("ASIENTO_YA_ASOCIADO", "El asiento N° " + asientoNumero + " ya está asociado a otro movimiento bancario");
        }

        m.setAsiento(asiento);
        m.setEstado(EstadoMovimientoBancario.CONCILIADO);

        auditoria.registrar(AccionAuditoria.CONFIRMAR, "MovimientoBancario", id, antes, mapper.aResponse(m));
        return m;
    }

    @Transactional
    public MovimientoBancario descartar(Long id, String motivo) {
        MovimientoBancario m = obtenerPendiente(id);
        var antes = mapper.aResponse(m);

        m.setEstado(EstadoMovimientoBancario.DESCARTADO);
        m.setMotivoDescarte(motivo);

        auditoria.registrar(AccionAuditoria.ANULAR, "MovimientoBancario", id, antes, mapper.aResponse(m));
        return m;
    }

    /** F5.2: filas sin fecha (ej. Galicia ARS) quedan pendientes hasta completarla con "corregir". */
    private void exigirFecha(MovimientoBancario m) {
        if (m.getFecha() == null) {
            throw new NegocioException("FECHA_PENDIENTE",
                    "Este movimiento no tiene fecha cargada — completala con \"corregir\" antes de continuar");
        }
    }

    private MovimientoBancario obtenerPendiente(Long id) {
        MovimientoBancario m = obtener(id);
        if (m.getEstado() != EstadoMovimientoBancario.PENDIENTE) {
            throw new NegocioException("MOVIMIENTO_NO_PENDIENTE", "Este movimiento ya fue revisado (" + m.getEstado() + ")");
        }
        return m;
    }

    private void aplicarCampos(MovimientoBancario m, Long cuentaBancariaId, LocalDate fecha, String descripcion,
            BigDecimal importe, Long monedaId, BigDecimal tipoCambio, String referencia,
            Long cuentaContableSugeridaId, String observaciones) {
        if (importe.compareTo(BigDecimal.ZERO) == 0) {
            throw new NegocioException("IMPORTE_CERO", "El importe del movimiento no puede ser cero");
        }

        m.setCuentaBancaria(resolverCuentaBancaria(cuentaBancariaId));
        m.setFecha(fecha);
        m.setDescripcion(descripcion);
        m.setImporte(importe);
        m.setMoneda(resolverMoneda(monedaId));
        m.setReferencia(referencia);
        m.setCuentaContableSugerida(cuentaContableSugeridaId != null ? resolverCuentaContable(cuentaContableSugeridaId) : null);
        m.setObservaciones(observaciones);

        if (MONEDA_LIBRO.equals(m.getMoneda().getCodigo())) {
            m.setTipoCambio(new BigDecimal("1.000000"));
            m.setFuenteTc(null);
            m.setImporteArs(importe);
        } else {
            m.setTipoCambio(tipoCambio);
            m.setFuenteTc(AsientoLinea.FuenteTc.MANUAL);
            m.setImporteArs(CalculoImputacion.round2(importe.multiply(tipoCambio)));
        }
    }

    /**
     * Asiento de 2 líneas: fondos (cuenta contable espejo de la cuenta
     * bancaria) vs. la contra-cuenta elegida. Un ingreso debita fondos y
     * acredita la contra-cuenta; un egreso es al revés — mismo criterio que
     * Cobro/Pago (F4.4), solo que acá el signo del movimiento decide el lado
     * en vez de que el generador lo fije de antemano.
     */
    private Asiento generarYRegistrarAsiento(MovimientoBancario m, CuentaContable contraCuenta) {
        CuentaContable cuentaFondos = m.getCuentaBancaria().getCuentaContable();
        boolean ingreso = m.getImporte().compareTo(BigDecimal.ZERO) > 0;
        BigDecimal montoArs = m.getImporteArs().abs();
        BigDecimal importeOriginalAbs = m.getImporte().abs();
        Long monedaId = m.getMoneda().getId();
        BigDecimal tc = m.getTipoCambio();
        String fuenteTc = m.getFuenteTc() != null ? m.getFuenteTc().name() : null;

        LineaAsientoGenerada lineaFondos = ingreso
                ? new LineaAsientoGenerada(cuentaFondos.getCodigo(), montoArs, BigDecimal.ZERO, m.getDescripcion(),
                        monedaId, importeOriginalAbs, tc, fuenteTc, null, null, null, null, m.getCuentaBancaria().getId())
                : new LineaAsientoGenerada(cuentaFondos.getCodigo(), BigDecimal.ZERO, montoArs, m.getDescripcion(),
                        monedaId, importeOriginalAbs, tc, fuenteTc, null, null, null, null, m.getCuentaBancaria().getId());
        LineaAsientoGenerada lineaContra = ingreso
                ? new LineaAsientoGenerada(contraCuenta.getCodigo(), BigDecimal.ZERO, montoArs, m.getDescripcion(),
                        monedaId, importeOriginalAbs, tc, fuenteTc, null, null, null, null, null)
                : new LineaAsientoGenerada(contraCuenta.getCodigo(), montoArs, BigDecimal.ZERO, m.getDescripcion(),
                        monedaId, importeOriginalAbs, tc, fuenteTc, null, null, null, null, null);

        AsientoGenerado generado = new AsientoGenerado(m.getFecha(), "Movimiento bancario - " + m.getDescripcion(),
                "MOVIMIENTO_BANCARIO", List.of(lineaFondos, lineaContra), "MovimientoBancario", m.getId());
        return asientoService.registrarAutomatico(generado);
    }

    private CuentaBancaria resolverCuentaBancaria(Long id) {
        return cuentaBancariaRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Cuenta bancaria " + id + " no encontrada"));
    }

    private Moneda resolverMoneda(Long id) {
        return monedaRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Moneda " + id + " no encontrada"));
    }

    private CuentaContable resolverCuentaContable(Long id) {
        return cuentaContableRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Cuenta contable " + id + " no encontrada"));
    }
}
