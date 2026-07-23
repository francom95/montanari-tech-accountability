package com.montanaritech.contable.contabilidad.estadoresultados;

import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.contabilidad.estadoresultados.dto.MapeoRubroLineaErDtos.CrearRequest;
import com.montanaritech.contable.contabilidad.estadoresultados.dto.MapeoRubroLineaErDtos.EditarRequest;
import com.montanaritech.contable.maestros.categoria.Categoria;
import com.montanaritech.contable.maestros.rubro.Rubro;
import com.montanaritech.contable.maestros.rubro.RubroRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD del mapeo rubro→línea del estado de resultados (F7.3), editable por ADMINISTRADOR. */
@Service
@RequiredArgsConstructor
public class MapeoRubroLineaEstadoResultadosService {

    private final MapeoRubroLineaEstadoResultadosRepository repo;
    private final RubroRepository rubroRepo;
    private final MapeoRubroLineaEstadoResultadosMapper mapper;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public List<MapeoRubroLineaEstadoResultados> listar() {
        return repo.listarOrdenadoPorRubro();
    }

    @Transactional(readOnly = true)
    public MapeoRubroLineaEstadoResultados obtener(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mapeo rubro-línea " + id + " no encontrado"));
    }

    @Transactional
    public MapeoRubroLineaEstadoResultados crear(CrearRequest req) {
        validarNaturaleza(req.naturaleza());
        MapeoRubroLineaEstadoResultados m = new MapeoRubroLineaEstadoResultados();
        m.setRubro(resolverRubro(req.rubroId()));
        m.setNaturaleza(req.naturaleza());
        m.setLinea(req.linea());
        try {
            MapeoRubroLineaEstadoResultados guardado = repo.save(m);
            auditoria.registrar(AccionAuditoria.CREAR, "MapeoRubroLineaEstadoResultados", guardado.getId(), null, mapper.aResponse(guardado));
            return guardado;
        } catch (DataIntegrityViolationException e) {
            throw new NegocioException("MAPEO_RUBRO_LINEA_DUPLICADO", "Ya existe un mapeo para ese rubro y esa naturaleza");
        }
    }

    @Transactional
    public MapeoRubroLineaEstadoResultados editar(Long id, EditarRequest req) {
        MapeoRubroLineaEstadoResultados m = obtener(id);
        var antes = mapper.aResponse(m);
        m.setLinea(req.linea());
        auditoria.registrar(AccionAuditoria.EDITAR, "MapeoRubroLineaEstadoResultados", id, antes, mapper.aResponse(m));
        return m;
    }

    @Transactional
    public void eliminar(Long id) {
        MapeoRubroLineaEstadoResultados m = obtener(id);
        var antes = mapper.aResponse(m);
        repo.delete(m);
        auditoria.registrar(AccionAuditoria.ELIMINAR, "MapeoRubroLineaEstadoResultados", id, antes, null);
    }

    private Rubro resolverRubro(Long id) {
        return rubroRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Rubro " + id + " no encontrado"));
    }

    private void validarNaturaleza(Categoria.TipoCategoria naturaleza) {
        if (naturaleza != Categoria.TipoCategoria.RP && naturaleza != Categoria.TipoCategoria.RN) {
            throw new NegocioException("NATURALEZA_INVALIDA_PARA_ER",
                    "Solo cuentas de naturaleza RP o RN entran al estado de resultados");
        }
    }
}
