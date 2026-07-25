package com.montanaritech.contable.inversion;

import com.montanaritech.contable.bancos.movimientobancario.MovimientoBancario;
import com.montanaritech.contable.bancos.movimientobancario.MovimientoBancarioService;
import com.montanaritech.contable.bancos.movimientobancario.dto.CrearMovimientoBancarioRequest;
import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.ConflictoException;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.inversion.dto.MovimientoInversionCrearRequest;
import com.montanaritech.contable.inversion.dto.MovimientoInversionResponse;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.tipocambio.TipoCambio;
import com.montanaritech.contable.maestros.tipocambio.TipoCambioRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Movimientos (suscripción/rescate) de una {@link Inversion} (F8.4). Crear un
 * movimiento genera automáticamente su {@link MovimientoBancario} en la
 * cuenta de origen (F5.1, no F4.4 — ver decisión de diseño de F8.4). Ledger
 * inmutable: sin edición ni eliminación, mismo criterio que
 * {@code ConsumoTarjeta}.
 */
@Service
@RequiredArgsConstructor
public class MovimientoInversionService {

    private final MovimientoInversionRepository repo;
    private final InversionService inversionService;
    private final MovimientoBancarioService movimientoBancarioService;
    private final TipoCambioRepository tipoCambioRepo;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public Page<MovimientoInversion> listar(Long inversionId, Pageable p) {
        return repo.findByInversion_IdOrderByFechaDesc(inversionId, p);
    }

    @Transactional(readOnly = true)
    public MovimientoInversion obtener(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Movimiento de inversión " + id + " no encontrado"));
    }

    @Transactional
    public MovimientoInversion crear(MovimientoInversionCrearRequest req) {
        Inversion inv = inversionService.obtener(req.inversionId());
        if (inv.getEstado() != EstadoInversion.ACTIVA) {
            throw new NegocioException("INVERSION_NO_ACTIVA",
                    "No se puede registrar un movimiento: la inversión no está activa (" + inv.getEstado() + ").");
        }

        BigDecimal cuotapartesActuales = inversionService.aResponse(inv).cuotapartesAcumuladas();
        if (req.tipo() == TipoMovimientoInversion.RESCATE && req.cuotapartes().compareTo(cuotapartesActuales) > 0) {
            throw new ConflictoException("CUOTAPARTES_INSUFICIENTES",
                    "No se puede rescatar " + req.cuotapartes() + " cuotapartes: la inversión solo tiene "
                            + cuotapartesActuales + ".");
        }

        MovimientoInversion m = new MovimientoInversion();
        m.setInversion(inv);
        m.setTipo(req.tipo());
        m.setFecha(req.fecha());
        m.setMontoAplicado(req.montoAplicado());
        m.setCuotapartes(req.cuotapartes());
        m.setValorCuotaparte(req.valorCuotaparte());
        m.setFechaLiquidacion(req.fechaLiquidacion());
        m.setObservaciones(req.observaciones());
        m.setMovimientoBancario(crearMovimientoBancario(inv, req));

        MovimientoInversion guardado = repo.save(m);

        if (req.tipo() == TipoMovimientoInversion.RESCATE) {
            BigDecimal restante = inversionService.aResponse(inv).cuotapartesAcumuladas();
            if (restante.compareTo(BigDecimal.ZERO) <= 0) {
                inv.setEstado(EstadoInversion.RESCATADA_TOTAL);
            }
        }

        auditoria.registrar(AccionAuditoria.CREAR, "MovimientoInversion", guardado.getId(), null, aResponse(guardado));
        return guardado;
    }

    /** F5.1, no F4.4: sin FK a Cliente/Proveedor ni asiento automático — ver decisión de diseño de F8.4. */
    private MovimientoBancario crearMovimientoBancario(Inversion inv, MovimientoInversionCrearRequest req) {
        Moneda monedaOrigen = inv.getCuentaOrigen().getMoneda();
        BigDecimal signo = req.tipo() == TipoMovimientoInversion.SUSCRIPCION ? BigDecimal.valueOf(-1) : BigDecimal.ONE;
        String descripcion = (req.tipo() == TipoMovimientoInversion.SUSCRIPCION ? "Suscripción " : "Rescate ")
                + inv.getInstrumento();
        return movimientoBancarioService.crear(new CrearMovimientoBancarioRequest(
                inv.getCuentaOrigen().getId(), req.fecha(), descripcion, req.montoAplicado().multiply(signo),
                monedaOrigen.getId(), resolverTipoCambio(monedaOrigen), null, null, null, null, null));
    }

    private BigDecimal resolverTipoCambio(Moneda moneda) {
        if ("ARS".equals(moneda.getCodigo())) {
            return BigDecimal.ONE;
        }
        return tipoCambioRepo.findFirstByMonedaIdAndActivoTrueOrderByFechaDesc(moneda.getId())
                .map(TipoCambio::getValorVenta)
                .orElseThrow(() -> new NegocioException("SIN_COTIZACION",
                        "No hay ninguna cotización cargada para " + moneda.getCodigo()
                                + " — cargá un tipo de cambio antes de registrar este movimiento."));
    }

    public MovimientoInversionResponse aResponse(MovimientoInversion m) {
        return new MovimientoInversionResponse(m.getId(), m.getInversion().getId(), m.getTipo(), m.getFecha(),
                m.getMontoAplicado(), m.getCuotapartes(), m.getValorCuotaparte(), m.getFechaLiquidacion(),
                m.getMovimientoBancario() != null ? m.getMovimientoBancario().getId() : null, m.getObservaciones());
    }
}
