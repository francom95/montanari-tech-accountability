package com.montanaritech.contable.bancos.tarjetacredito;

import com.montanaritech.contable.bancos.tarjetacredito.dto.PagoTarjetaCrearRequest;
import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.common.estado.TransicionEstadoValidator;
import com.montanaritech.contable.common.saldo.RecalculoSaldoService;
import com.montanaritech.contable.contabilidad.asiento.Asiento;
import com.montanaritech.contable.contabilidad.asiento.AsientoService;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import com.montanaritech.contable.maestros.tarjetacredito.TarjetaCredito;
import com.montanaritech.contable.maestros.tarjetacredito.TarjetaCreditoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pago del resumen de tarjeta (F5.4 §3): puede ser parcial (pago mínimo) —
 * el saldo pendiente sale de {@code RecalculoSaldoService}, no hay campo ni
 * lógica especial acá. Al confirmar, genera el asiento (PL-4,
 * {@code PagoTarjetaAsientoGenerator}) con la cuenta bancaria de débito de
 * la tarjeta, lo que lo hace automáticamente conciliable por F5.3.
 */
@Service
@RequiredArgsConstructor
public class PagoTarjetaService {

    private static final String MONEDA_LIBRO = "ARS";

    private final PagoTarjetaRepository repo;
    private final TarjetaCreditoRepository tarjetaCreditoRepository;
    private final MonedaRepository monedaRepository;
    private final PagoTarjetaMapper mapper;
    private final AuditoriaService auditoria;
    private final AsientoService asientoService;
    private final PagoTarjetaAsientoGenerator generator;
    private final RecalculoSaldoService recalculoSaldoService;

    @Transactional(readOnly = true)
    public Page<PagoTarjeta> listar(Long tarjetaCreditoId, Pageable p) {
        return repo.buscar(tarjetaCreditoId, p);
    }

    @Transactional(readOnly = true)
    public PagoTarjeta obtener(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Pago de tarjeta " + id + " no encontrado"));
    }

    @Transactional
    public PagoTarjeta crearBorrador(PagoTarjetaCrearRequest req) {
        TarjetaCredito tarjeta = resolverTarjeta(req.tarjetaCreditoId());
        Moneda moneda = resolverMoneda(req.monedaId());

        if (req.importe().compareTo(BigDecimal.ZERO) <= 0) {
            throw new NegocioException("PAGO_TARJETA_SIN_IMPORTE", "El importe del pago debe ser mayor a cero");
        }

        PagoTarjeta p = new PagoTarjeta();
        p.setTarjetaCredito(tarjeta);
        p.setFecha(req.fecha());
        p.setImporte(req.importe());
        p.setMoneda(moneda);
        p.setObservaciones(req.observaciones());
        aplicarTipoCambio(p, moneda, req.tipoCambio());
        p.setEstado(EstadoDocumento.BORRADOR);

        PagoTarjeta guardado = repo.save(p);
        auditoria.registrar(AccionAuditoria.CREAR, "PagoTarjeta", guardado.getId(), null, mapper.aResponse(guardado));
        return guardado;
    }

    @Transactional
    public PagoTarjeta confirmar(Long id) {
        PagoTarjeta p = obtener(id);
        var antes = mapper.aResponse(p);
        TransicionEstadoValidator.validar(p.getEstado(), EstadoDocumento.CONFIRMADO);

        var asientoGenerado = generator.generar(p);
        Asiento asiento = asientoService.registrarAutomatico(asientoGenerado);

        p.setAsiento(asiento);
        p.setEstado(EstadoDocumento.CONFIRMADO);
        recalculoSaldoService.recalcular(p.getTarjetaCredito());

        auditoria.registrar(AccionAuditoria.CONFIRMAR, "PagoTarjeta", id, antes, mapper.aResponse(p));
        return p;
    }

    @Transactional
    public PagoTarjeta anular(Long id, String motivo) {
        PagoTarjeta p = obtener(id);
        var antes = mapper.aResponse(p);
        TransicionEstadoValidator.validar(p.getEstado(), EstadoDocumento.ANULADO);

        boolean estabaConfirmado = p.getEstado() == EstadoDocumento.CONFIRMADO;
        if (estabaConfirmado) {
            asientoService.anularPorDocumento(p.getAsiento().getId(), motivo);
        }
        p.setEstado(EstadoDocumento.ANULADO);
        if (estabaConfirmado) {
            recalculoSaldoService.recalcular(p.getTarjetaCredito());
        }

        auditoria.registrar(AccionAuditoria.ANULAR, "PagoTarjeta", id, antes, mapper.aResponse(p));
        return p;
    }

    private void aplicarTipoCambio(PagoTarjeta p, Moneda moneda, BigDecimal tipoCambio) {
        if (MONEDA_LIBRO.equals(moneda.getCodigo())) {
            p.setTipoCambio(BigDecimal.ONE);
            p.setImporteArs(p.getImporte());
        } else {
            p.setTipoCambio(tipoCambio);
            p.setImporteArs(p.getImporte().multiply(tipoCambio).setScale(2, RoundingMode.HALF_UP));
        }
    }

    private TarjetaCredito resolverTarjeta(Long id) {
        return tarjetaCreditoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Tarjeta de crédito " + id + " no encontrada"));
    }

    private Moneda resolverMoneda(Long id) {
        return monedaRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Moneda " + id + " no encontrada"));
    }
}
