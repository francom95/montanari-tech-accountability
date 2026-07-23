package com.montanaritech.contable.bancos.tarjetacredito;

import com.montanaritech.contable.bancos.tarjetacredito.dto.ClasificarConsumoRequest;
import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContableRepository;
import com.montanaritech.contable.maestros.concepto.Concepto;
import com.montanaritech.contable.maestros.concepto.ConceptoRepository;
import com.montanaritech.contable.maestros.proveedor.Proveedor;
import com.montanaritech.contable.maestros.proveedor.ProveedorRepository;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumos de tarjeta (F5.4): clasificación manual (cuenta + proveedor/
 * proyecto/concepto opcionales) o masiva por {@link ReglaClasificacionConsumo}
 * (primera regla activa cuya {@code patron} aparece en la descripción,
 * orden alfabético de patrón — determinístico, sin prioridad configurable
 * todavía: alcance mínimo de F5.4, "reglas simples").
 */
@Service
@RequiredArgsConstructor
public class ConsumoTarjetaService {

    private final ConsumoTarjetaRepository repo;
    private final ReglaClasificacionConsumoRepository reglaRepo;
    private final CuentaContableRepository cuentaContableRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProyectoRepository proyectoRepository;
    private final ConceptoRepository conceptoRepository;
    private final ConsumoTarjetaMapper mapper;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public Page<ConsumoTarjeta> listar(Long tarjetaCreditoId, boolean soloSinClasificar, Pageable p) {
        return repo.buscar(tarjetaCreditoId, soloSinClasificar, p);
    }

    @Transactional(readOnly = true)
    public ConsumoTarjeta obtener(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Consumo de tarjeta " + id + " no encontrado"));
    }

    @Transactional(readOnly = true)
    public long contarSinClasificar(Long tarjetaCreditoId) {
        return repo.countByTarjetaCredito_IdAndCuentaContableIsNull(tarjetaCreditoId);
    }

    @Transactional
    public ConsumoTarjeta clasificar(Long id, ClasificarConsumoRequest req) {
        ConsumoTarjeta c = obtener(id);
        var antes = mapper.aResponse(c);

        c.setCuentaContable(resolverCuentaContable(req.cuentaContableId()));
        c.setProveedor(req.proveedorId() != null ? resolverProveedor(req.proveedorId()) : null);
        c.setProyecto(req.proyectoId() != null ? resolverProyecto(req.proyectoId()) : null);
        c.setConcepto(req.conceptoId() != null ? resolverConcepto(req.conceptoId()) : null);

        auditoria.registrar(AccionAuditoria.EDITAR, "ConsumoTarjeta", id, antes, mapper.aResponse(c));
        return c;
    }

    /** @return cantidad de consumos clasificados por alguna regla activa. */
    @Transactional
    public int clasificarMasivamente(Long tarjetaCreditoId) {
        List<ConsumoTarjeta> sinClasificar = repo.findByTarjetaCredito_IdAndCuentaContableIsNull(tarjetaCreditoId);
        if (sinClasificar.isEmpty()) {
            return 0;
        }
        List<ReglaClasificacionConsumo> reglas = reglaRepo.findByActivoTrue();
        if (reglas.isEmpty()) {
            return 0;
        }

        int clasificados = 0;
        for (ConsumoTarjeta consumo : sinClasificar) {
            String descripcionNormalizada = consumo.getDescripcion().toUpperCase(Locale.ROOT);
            ReglaClasificacionConsumo regla = reglas.stream()
                    .filter(r -> descripcionNormalizada.contains(r.getPatron().toUpperCase(Locale.ROOT)))
                    .findFirst()
                    .orElse(null);
            if (regla == null) {
                continue;
            }
            var antes = mapper.aResponse(consumo);
            consumo.setCuentaContable(regla.getCuentaContable());
            consumo.setProveedor(regla.getProveedor());
            consumo.setProyecto(regla.getProyecto());
            consumo.setConcepto(regla.getConcepto());
            auditoria.registrar(AccionAuditoria.EDITAR, "ConsumoTarjeta", consumo.getId(), antes, mapper.aResponse(consumo));
            clasificados++;
        }
        return clasificados;
    }

    private CuentaContable resolverCuentaContable(Long id) {
        return cuentaContableRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Cuenta contable " + id + " no encontrada"));
    }

    private Proveedor resolverProveedor(Long id) {
        return proveedorRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Proveedor " + id + " no encontrado"));
    }

    private Proyecto resolverProyecto(Long id) {
        return proyectoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Proyecto " + id + " no encontrado"));
    }

    private Concepto resolverConcepto(Long id) {
        return conceptoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Concepto " + id + " no encontrado"));
    }
}
