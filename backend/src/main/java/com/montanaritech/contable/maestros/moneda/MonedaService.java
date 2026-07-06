package com.montanaritech.contable.maestros.moneda;

import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.Auditado;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.ConflictoException;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.maestros.moneda.dto.MonedaCrearRequest;
import com.montanaritech.contable.maestros.moneda.dto.MonedaEditarRequest;
import com.montanaritech.contable.maestros.moneda.dto.MonedaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Molde de referencia de PL-1 (F1.8). Para aplicarlo a una entidad nueva:
 * <ol>
 *   <li>{@code listar}/{@code obtener}: copiar tal cual, cambiar el tipo.</li>
 *   <li>{@code crear}: validar unicidad de la clave natural propia (acá,
 *       {@code codigo}), {@code @Auditado} en el método (solo importa el
 *       resultado — no hay "antes").</li>
 *   <li>{@code editar}/{@code activar}/{@code desactivar}: snapshot del DTO
 *       "antes" ANTES de mutar, llamar a {@code AuditoriaService.registrar}
 *       con antes/después explícito (si automatizás esto con AOP perdés el
 *       "antes" real — ver nota en {@code Auditado}).</li>
 *   <li>{@code eliminar}: {@code tieneMovimientosAsociados} es el punto de
 *       extensión — cuando la entidad nueva tenga hijos reales (facturas,
 *       asientos, etc. la referencian por FK), ese método pasa a consultar
 *       esos repositorios. Acá, Moneda todavía no tiene ningún hijo en el
 *       sistema, así que siempre devuelve {@code false}.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class MonedaService {

    private final MonedaRepository monedaRepository;
    private final MonedaMapper monedaMapper;
    private final AuditoriaService auditoriaService;

    @Transactional(readOnly = true)
    public Page<Moneda> listar(String texto, Boolean activo, Pageable pageable) {
        return monedaRepository.buscar(texto, activo, pageable);
    }

    @Transactional(readOnly = true)
    public Moneda obtener(Long id) {
        return monedaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Moneda " + id + " no encontrada"));
    }

    @Auditado(accion = AccionAuditoria.CREAR, entidadTipo = "Moneda")
    @Transactional
    public Moneda crear(MonedaCrearRequest request) {
        monedaRepository.findByCodigo(request.codigo()).ifPresent(m -> {
            throw new ConflictoException("CODIGO_DUPLICADO", "Ya existe una moneda con ese código");
        });

        Moneda moneda = new Moneda();
        moneda.setCodigo(request.codigo());
        moneda.setNombre(request.nombre());
        moneda.setSimbolo(request.simbolo());
        moneda.setActivo(true);
        return monedaRepository.save(moneda);
    }

    @Transactional
    public Moneda editar(Long id, MonedaEditarRequest request) {
        Moneda moneda = obtener(id);
        MonedaResponse antes = monedaMapper.aResponse(moneda);

        moneda.setNombre(request.nombre());
        moneda.setSimbolo(request.simbolo());

        auditoriaService.registrar(
                AccionAuditoria.EDITAR, "Moneda", moneda.getId(), antes, monedaMapper.aResponse(moneda));
        return moneda;
    }

    @Transactional
    public Moneda desactivar(Long id) {
        Moneda moneda = obtener(id);
        MonedaResponse antes = monedaMapper.aResponse(moneda);
        moneda.setActivo(false);
        auditoriaService.registrar(
                AccionAuditoria.CAMBIO_ESTADO, "Moneda", moneda.getId(), antes, monedaMapper.aResponse(moneda));
        return moneda;
    }

    @Transactional
    public Moneda activar(Long id) {
        Moneda moneda = obtener(id);
        MonedaResponse antes = monedaMapper.aResponse(moneda);
        moneda.setActivo(true);
        auditoriaService.registrar(
                AccionAuditoria.CAMBIO_ESTADO, "Moneda", moneda.getId(), antes, monedaMapper.aResponse(moneda));
        return moneda;
    }

    @Transactional
    public void eliminar(Long id) {
        Moneda moneda = obtener(id);
        if (tieneMovimientosAsociados(id)) {
            throw new ConflictoException(
                    "TIENE_MOVIMIENTOS_ASOCIADOS",
                    "No se puede eliminar: la moneda tiene movimientos asociados. Desactivala en su lugar.");
        }
        MonedaResponse antes = monedaMapper.aResponse(moneda);
        monedaRepository.delete(moneda);
        auditoriaService.registrar(AccionAuditoria.ELIMINAR, "Moneda", id, antes, null);
    }

    /**
     * Punto de extensión (ver Javadoc de la clase): hoy ninguna otra
     * entidad referencia Moneda por FK, así que nunca bloquea. Cuando F2+
     * agregue entidades que sí la referencien, este método debe consultar
     * esos repositorios (p. ej. {@code facturaRepository.existsByMonedaId(id)}).
     */
    private boolean tieneMovimientosAsociados(Long monedaId) {
        return false;
    }
}
