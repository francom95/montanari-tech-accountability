package com.montanaritech.contable.maestros.tarjetacredito;

import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.Auditado;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.common.saldo.RecalculoSaldoService;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContableRepository;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancaria;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancariaRepository;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import com.montanaritech.contable.maestros.tarjetacredito.dto.TarjetaCreditoCrearRequest;
import com.montanaritech.contable.maestros.tarjetacredito.dto.TarjetaCreditoEditarRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TarjetaCreditoService {

    private final TarjetaCreditoRepository repo;
    private final MonedaRepository monedaRepository;
    private final CuentaBancariaRepository cuentaBancariaRepository;
    private final CuentaContableRepository cuentaContableRepository;
    private final TarjetaCreditoMapper mapper;
    private final AuditoriaService auditoria;
    private final RecalculoSaldoService recalculoSaldoService;

    @Transactional(readOnly = true)
    public Page<TarjetaCredito> listar(String texto, Boolean activo, Pageable p) {
        return repo.buscar(texto, activo, p);
    }

    @Transactional(readOnly = true)
    public TarjetaCredito obtener(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tarjeta de crédito " + id + " no encontrada"));
    }

    @Auditado(accion = AccionAuditoria.CREAR, entidadTipo = "TarjetaCredito")
    @Transactional
    public TarjetaCredito crear(TarjetaCreditoCrearRequest req) {
        Moneda moneda = resolverMoneda(req.monedaId());
        CuentaBancaria cuentaDebito = resolverCuentaDebito(req.cuentaBancariaDebitoId());
        CuentaContable cuentaContable = resolverCuentaContable(req.cuentaContableId());

        TarjetaCredito e = new TarjetaCredito();
        e.setEntidad(req.entidad());
        e.setMoneda(moneda);
        e.setDiaCierre(req.diaCierre());
        e.setDiaVencimiento(req.diaVencimiento());
        e.setCuentaBancariaDebito(cuentaDebito);
        e.setCuentaContable(cuentaContable);
        e.setSaldoInicial(req.saldoInicial());
        e.setFechaSaldoInicial(req.fechaSaldoInicial());
        e.setActivo(true);
        recalculoSaldoService.recalcular(e);
        return repo.save(e);
    }

    @Transactional
    public TarjetaCredito editar(Long id, TarjetaCreditoEditarRequest req) {
        TarjetaCredito e = obtener(id);
        var antes = mapper.aResponse(e);

        Moneda moneda = resolverMoneda(req.monedaId());
        CuentaBancaria cuentaDebito = resolverCuentaDebito(req.cuentaBancariaDebitoId());
        CuentaContable cuentaContable = resolverCuentaContable(req.cuentaContableId());

        e.setEntidad(req.entidad());
        e.setMoneda(moneda);
        e.setDiaCierre(req.diaCierre());
        e.setDiaVencimiento(req.diaVencimiento());
        e.setCuentaBancariaDebito(cuentaDebito);
        e.setCuentaContable(cuentaContable);
        e.setSaldoInicial(req.saldoInicial());
        e.setFechaSaldoInicial(req.fechaSaldoInicial());
        recalculoSaldoService.recalcular(e);

        auditoria.registrar(AccionAuditoria.EDITAR, "TarjetaCredito", id, antes, mapper.aResponse(e));
        return e;
    }

    @Transactional
    public TarjetaCredito desactivar(Long id) {
        TarjetaCredito e = obtener(id);
        var antes = mapper.aResponse(e);
        e.setActivo(false);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "TarjetaCredito", id, antes, mapper.aResponse(e));
        return e;
    }

    @Transactional
    public TarjetaCredito activar(Long id) {
        TarjetaCredito e = obtener(id);
        var antes = mapper.aResponse(e);
        e.setActivo(true);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "TarjetaCredito", id, antes, mapper.aResponse(e));
        return e;
    }

    @Transactional
    public void eliminar(Long id) {
        TarjetaCredito e = obtener(id);
        var antes = mapper.aResponse(e);
        repo.delete(e);
        auditoria.registrar(AccionAuditoria.ELIMINAR, "TarjetaCredito", id, antes, null);
    }

    private Moneda resolverMoneda(Long monedaId) {
        return monedaRepository.findById(monedaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Moneda " + monedaId + " no encontrada"));
    }

    private CuentaBancaria resolverCuentaDebito(Long cuentaBancariaId) {
        return cuentaBancariaRepository.findById(cuentaBancariaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta bancaria " + cuentaBancariaId + " no encontrada"));
    }

    private CuentaContable resolverCuentaContable(Long cuentaContableId) {
        return cuentaContableRepository.findById(cuentaContableId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta contable " + cuentaContableId + " no encontrada"));
    }
}
