package com.montanaritech.contable.maestros.comisionista;

import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.Auditado;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.ConflictoException;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.maestros.comisionista.dto.ComisionistaCrearRequest;
import com.montanaritech.contable.maestros.comisionista.dto.ComisionistaEditarRequest;
import com.montanaritech.contable.maestros.proyecto.comision.ComisionProyectoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ComisionistaService {
    private final ComisionistaRepository repo;
    private final ComisionistaMapper mapper;
    private final AuditoriaService auditoria;
    private final ComisionProyectoRepository comisionProyectoRepo;

    @Transactional(readOnly = true)
    public Page<Comisionista> listar(String texto, Boolean activo, Pageable p) {
        return repo.buscar(texto, activo, p);
    }

    @Transactional(readOnly = true)
    public Comisionista obtener(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Comisionista " + id + " no encontrado"));
    }

    @Auditado(accion = AccionAuditoria.CREAR, entidadTipo = "Comisionista")
    @Transactional
    public Comisionista crear(ComisionistaCrearRequest req) {
        Comisionista c = new Comisionista();
        c.setNombre(req.nombre());
        c.setCuit(normalizarCuit(req.cuit()));
        c.setContacto(req.contacto());
        c.setEmail(req.email());
        c.setTelefono(req.telefono());
        c.setActivo(true);
        return repo.save(c);
    }

    @Transactional
    public Comisionista editar(Long id, ComisionistaEditarRequest req) {
        Comisionista c = obtener(id);
        var antes = mapper.aResponse(c);

        c.setNombre(req.nombre());
        c.setCuit(normalizarCuit(req.cuit()));
        c.setContacto(req.contacto());
        c.setEmail(req.email());
        c.setTelefono(req.telefono());

        auditoria.registrar(AccionAuditoria.EDITAR, "Comisionista", id, antes, mapper.aResponse(c));
        return c;
    }

    @Transactional
    public Comisionista desactivar(Long id) {
        Comisionista c = obtener(id);
        var antes = mapper.aResponse(c);
        c.setActivo(false);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "Comisionista", id, antes, mapper.aResponse(c));
        return c;
    }

    @Transactional
    public Comisionista activar(Long id) {
        Comisionista c = obtener(id);
        var antes = mapper.aResponse(c);
        c.setActivo(true);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "Comisionista", id, antes, mapper.aResponse(c));
        return c;
    }

    @Transactional
    public void eliminar(Long id) {
        Comisionista c = obtener(id);
        if (comisionProyectoRepo.existsByComisionistaId(id)) {
            throw new ConflictoException("COMISIONES_ASOCIADAS", "No se puede eliminar el comisionista: tiene comisiones de proyecto asociadas");
        }
        var antes = mapper.aResponse(c);
        repo.delete(c);
        auditoria.registrar(AccionAuditoria.ELIMINAR, "Comisionista", id, antes, null);
    }

    private String normalizarCuit(String cuit) {
        return (cuit == null || cuit.isBlank()) ? null : cuit;
    }
}
