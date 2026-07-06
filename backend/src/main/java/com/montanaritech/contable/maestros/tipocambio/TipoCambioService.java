package com.montanaritech.contable.maestros.tipocambio;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.audit.Auditado;
import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import com.montanaritech.contable.maestros.tipocambio.dto.TipoCambioCrearRequest;
import com.montanaritech.contable.maestros.tipocambio.dto.TipoCambioEditarRequest;
import com.montanaritech.contable.maestros.tipocambio.dto.TipoCambioResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
public class TipoCambioService {
    private final TipoCambioRepository repo;
    private final MonedaRepository monedaRepository;
    private final TipoCambioMapper mapper;
    private final AuditoriaService auditoria;
    @Transactional(readOnly = true)
    public Page<TipoCambio> listar(String texto, Boolean activo, Pageable p) {
        return repo.buscar(texto, activo, p);
    }
    @Transactional(readOnly = true)
    public TipoCambio obtener(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("TipoCambio " + id + " no encontrado"));
    }
    @Auditado(accion = AccionAuditoria.CREAR, entidadTipo = "TipoCambio")
    @Transactional
    public TipoCambio crear(TipoCambioCrearRequest req) {
        Moneda moneda = monedaRepository.findById(req.monedaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Moneda " + req.monedaId() + " no encontrada"));
        TipoCambio tc = new TipoCambio();
        tc.setFecha(req.fecha());
        tc.setMoneda(moneda);
        tc.setCriterio(req.criterio());
        tc.setValorCompra(req.valorCompra());
        tc.setValorVenta(req.valorVenta());
        tc.setFuente(req.fuente());
        tc.setObservaciones(req.observaciones());
        tc.setActivo(true);
        return repo.save(tc);
    }
    @Transactional
    public TipoCambio editar(Long id, TipoCambioEditarRequest req) {
        TipoCambio tc = obtener(id);
        TipoCambioResponse antes = mapper.aResponse(tc);
        tc.setValorCompra(req.valorCompra());
        tc.setValorVenta(req.valorVenta());
        tc.setFuente(req.fuente());
        tc.setObservaciones(req.observaciones());
        auditoria.registrar(AccionAuditoria.EDITAR, "TipoCambio", id, antes, mapper.aResponse(tc));
        return tc;
    }
    @Transactional
    public TipoCambio desactivar(Long id) {
        TipoCambio tc = obtener(id);
        TipoCambioResponse antes = mapper.aResponse(tc);
        tc.setActivo(false);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "TipoCambio", id, antes, mapper.aResponse(tc));
        return tc;
    }
    @Transactional
    public TipoCambio activar(Long id) {
        TipoCambio tc = obtener(id);
        TipoCambioResponse antes = mapper.aResponse(tc);
        tc.setActivo(true);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "TipoCambio", id, antes, mapper.aResponse(tc));
        return tc;
    }
    @Transactional
    public void eliminar(Long id) {
        TipoCambio tc = obtener(id);
        TipoCambioResponse antes = mapper.aResponse(tc);
        repo.delete(tc);
        auditoria.registrar(AccionAuditoria.ELIMINAR, "TipoCambio", id, antes, null);
    }
}
