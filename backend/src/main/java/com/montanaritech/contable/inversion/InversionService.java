package com.montanaritech.contable.inversion;

import com.montanaritech.contable.common.asiento.CalculoImputacion;
import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.ConflictoException;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.compromiso.CompromisoService;
import com.montanaritech.contable.inversion.dto.InversionCrearRequest;
import com.montanaritech.contable.inversion.dto.InversionEditarRequest;
import com.montanaritech.contable.inversion.dto.InversionResponse;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancaria;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancariaRepository;
import com.montanaritech.contable.vencimientos.VencimientoService;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD de {@link Inversion} (F8.4), molde PL-1 sobre F1.8. */
@Service
@RequiredArgsConstructor
public class InversionService {

    private final InversionRepository repo;
    private final MovimientoInversionRepository movimientoRepo;
    private final CuentaBancariaRepository cuentaBancariaRepo;
    private final CompromisoService compromisoService;
    private final VencimientoService vencimientoService;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public Page<Inversion> listar(String texto, EstadoInversion estado, Boolean activo, Pageable p) {
        return repo.buscar(texto, estado, activo, p);
    }

    /** F8.4: fuente para que F8.3 proyecte el rescate planificado de las inversiones vinculadas. */
    @Transactional(readOnly = true)
    public List<Inversion> buscarActivasConVinculo() {
        return repo.findByActivoTrueAndEstadoAndVinculoTipoIsNotNull(EstadoInversion.ACTIVA);
    }

    @Transactional(readOnly = true)
    public Inversion obtener(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Inversión " + id + " no encontrada"));
    }

    @Transactional
    public Inversion crear(InversionCrearRequest req) {
        Inversion inv = new Inversion();
        inv.setInstrumento(req.instrumento());
        inv.setCuentaOrigen(resolverCuentaBancaria(req.cuentaOrigenId()));
        inv.setObjetivoDelDinero(req.objetivoDelDinero());
        aplicarVinculo(inv, req.vinculoTipo(), req.vinculoRefId());
        inv.setActivo(true);
        Inversion guardado = repo.save(inv);
        auditoria.registrar(AccionAuditoria.CREAR, "Inversion", guardado.getId(), null, aResponse(guardado));
        return guardado;
    }

    @Transactional
    public Inversion editar(Long id, InversionEditarRequest req) {
        Inversion inv = obtener(id);
        var antes = aResponse(inv);
        inv.setInstrumento(req.instrumento());
        inv.setCuentaOrigen(resolverCuentaBancaria(req.cuentaOrigenId()));
        inv.setObjetivoDelDinero(req.objetivoDelDinero());
        aplicarVinculo(inv, req.vinculoTipo(), req.vinculoRefId());
        inv.setEstado(req.estado());
        auditoria.registrar(AccionAuditoria.EDITAR, "Inversion", id, antes, aResponse(inv));
        return inv;
    }

    @Transactional
    public Inversion activar(Long id) {
        Inversion inv = obtener(id);
        var antes = aResponse(inv);
        inv.setActivo(true);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "Inversion", id, antes, aResponse(inv));
        return inv;
    }

    @Transactional
    public Inversion desactivar(Long id) {
        Inversion inv = obtener(id);
        var antes = aResponse(inv);
        inv.setActivo(false);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "Inversion", id, antes, aResponse(inv));
        return inv;
    }

    @Transactional
    public void eliminar(Long id) {
        Inversion inv = obtener(id);
        if (movimientoRepo.countByInversion_Id(id) > 0) {
            throw new ConflictoException("MOVIMIENTOS_ASOCIADOS",
                    "No se puede eliminar: ya tiene movimientos de suscripción/rescate asociados.");
        }
        var antes = aResponse(inv);
        repo.delete(inv);
        auditoria.registrar(AccionAuditoria.ELIMINAR, "Inversion", id, antes, null);
    }

    /** Valida que el vínculo (si se seteó) apunte a un Compromiso/Vencimiento existente — sin FK, resuelto acá. */
    private void aplicarVinculo(Inversion inv, TipoVinculoInversion tipo, Long refId) {
        if (tipo == null || refId == null) {
            inv.setVinculoTipo(null);
            inv.setVinculoRefId(null);
            return;
        }
        switch (tipo) {
            case COMPROMISO -> compromisoService.obtener(refId);
            case VENCIMIENTO -> vencimientoService.obtener(refId);
        }
        inv.setVinculoTipo(tipo);
        inv.setVinculoRefId(refId);
    }

    private CuentaBancaria resolverCuentaBancaria(Long id) {
        return cuentaBancariaRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta bancaria " + id + " no encontrada"));
    }

    /**
     * Cálculos derivados desde {@link MovimientoInversion} — sin columnas
     * persistidas, evita staleness. {@code valuacionActual} usa el
     * {@code valorCuotaparte} del movimiento más reciente por fecha
     * (decisión F8.4).
     */
    public InversionResponse aResponse(Inversion inv) {
        List<MovimientoInversion> movimientos = movimientoRepo.findByInversion_Id(inv.getId());
        BigDecimal cuotapartesAcumuladas = BigDecimal.ZERO;
        BigDecimal montoNetoAplicado = BigDecimal.ZERO;
        for (MovimientoInversion m : movimientos) {
            BigDecimal signo = m.getTipo() == TipoMovimientoInversion.SUSCRIPCION ? BigDecimal.ONE : BigDecimal.valueOf(-1);
            cuotapartesAcumuladas = cuotapartesAcumuladas.add(m.getCuotapartes().multiply(signo));
            montoNetoAplicado = montoNetoAplicado.add(m.getMontoAplicado().multiply(signo));
        }
        BigDecimal ultimoValorCuotaparte = movimientoRepo.findFirstByInversion_IdOrderByFechaDescIdDesc(inv.getId())
                .map(MovimientoInversion::getValorCuotaparte)
                .orElse(BigDecimal.ZERO);
        BigDecimal valuacionActual = CalculoImputacion.round2(cuotapartesAcumuladas.multiply(ultimoValorCuotaparte));
        BigDecimal rendimiento = CalculoImputacion.round2(valuacionActual.subtract(montoNetoAplicado));
        return new InversionResponse(inv.getId(), inv.getInstrumento(), inv.getCuentaOrigen().getId(),
                inv.getCuentaOrigen().getAlias(), inv.getObjetivoDelDinero(), inv.getVinculoTipo(), inv.getVinculoRefId(),
                inv.getEstado(), inv.isActivo(), cuotapartesAcumuladas, montoNetoAplicado, valuacionActual, rendimiento);
    }
}
