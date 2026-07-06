package com.montanaritech.contable.maestros.cuentabancaria;

import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.Auditado;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.common.saldo.RecalculoSaldoService;
import com.montanaritech.contable.maestros.cuentabancaria.dto.CuentaBancariaCrearRequest;
import com.montanaritech.contable.maestros.cuentabancaria.dto.CuentaBancariaEditarRequest;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CuentaBancariaService {

    private final CuentaBancariaRepository repo;
    private final MonedaRepository monedaRepository;
    private final CuentaBancariaMapper mapper;
    private final AuditoriaService auditoria;
    private final RecalculoSaldoService recalculoSaldoService;

    @Transactional(readOnly = true)
    public Page<CuentaBancaria> listar(String texto, Boolean activo, Pageable p) {
        return repo.buscar(texto, activo, p);
    }

    @Transactional(readOnly = true)
    public CuentaBancaria obtener(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta bancaria " + id + " no encontrada"));
    }

    @Auditado(accion = AccionAuditoria.CREAR, entidadTipo = "CuentaBancaria")
    @Transactional
    public CuentaBancaria crear(CuentaBancariaCrearRequest req) {
        Moneda moneda = resolverMoneda(req.monedaId());

        CuentaBancaria e = new CuentaBancaria();
        e.setEntidad(req.entidad());
        e.setAlias(req.alias());
        e.setMoneda(moneda);
        e.setTipo(CuentaBancaria.TipoCuenta.valueOf(req.tipo()));
        e.setEstadoConciliacion(
                req.estadoConciliacion() != null
                        ? CuentaBancaria.EstadoConciliacion.valueOf(req.estadoConciliacion())
                        : CuentaBancaria.EstadoConciliacion.PENDIENTE);
        e.setSaldoInicial(req.saldoInicial());
        e.setFechaSaldoInicial(req.fechaSaldoInicial());
        e.setActivo(true);
        recalculoSaldoService.recalcular(e);
        return repo.save(e);
    }

    @Transactional
    public CuentaBancaria editar(Long id, CuentaBancariaEditarRequest req) {
        CuentaBancaria e = obtener(id);
        var antes = mapper.aResponse(e);

        Moneda moneda = resolverMoneda(req.monedaId());
        e.setEntidad(req.entidad());
        e.setAlias(req.alias());
        e.setMoneda(moneda);
        e.setTipo(CuentaBancaria.TipoCuenta.valueOf(req.tipo()));
        e.setEstadoConciliacion(
                req.estadoConciliacion() != null
                        ? CuentaBancaria.EstadoConciliacion.valueOf(req.estadoConciliacion())
                        : e.getEstadoConciliacion());
        e.setSaldoInicial(req.saldoInicial());
        e.setFechaSaldoInicial(req.fechaSaldoInicial());
        recalculoSaldoService.recalcular(e);

        auditoria.registrar(AccionAuditoria.EDITAR, "CuentaBancaria", id, antes, mapper.aResponse(e));
        return e;
    }

    @Transactional
    public CuentaBancaria desactivar(Long id) {
        CuentaBancaria e = obtener(id);
        var antes = mapper.aResponse(e);
        e.setActivo(false);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "CuentaBancaria", id, antes, mapper.aResponse(e));
        return e;
    }

    @Transactional
    public CuentaBancaria activar(Long id) {
        CuentaBancaria e = obtener(id);
        var antes = mapper.aResponse(e);
        e.setActivo(true);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "CuentaBancaria", id, antes, mapper.aResponse(e));
        return e;
    }

    @Transactional
    public void eliminar(Long id) {
        CuentaBancaria e = obtener(id);
        var antes = mapper.aResponse(e);
        repo.delete(e);
        auditoria.registrar(AccionAuditoria.ELIMINAR, "CuentaBancaria", id, antes, null);
    }

    private Moneda resolverMoneda(Long monedaId) {
        return monedaRepository.findById(monedaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Moneda " + monedaId + " no encontrada"));
    }
}
