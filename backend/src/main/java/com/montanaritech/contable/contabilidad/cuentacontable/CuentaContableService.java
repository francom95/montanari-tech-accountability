package com.montanaritech.contable.contabilidad.cuentacontable;

import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.Auditado;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.ConflictoException;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.contabilidad.asiento.AsientoLineaRepository;
import com.montanaritech.contable.contabilidad.cuentacontable.dto.CuentaContableCrearRequest;
import com.montanaritech.contable.contabilidad.cuentacontable.dto.CuentaContableEditarRequest;
import com.montanaritech.contable.contabilidad.cuentacontable.dto.CuentaContableNodo;
import com.montanaritech.contable.maestros.categoria.Categoria;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import com.montanaritech.contable.maestros.rubro.Rubro;
import com.montanaritech.contable.maestros.rubro.RubroRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Plan de cuentas (F3.2), sobre las decisiones de F3.1 §2. {@code tieneMovimientos}
 * consulta {@code asiento_linea} (F3.4): una cuenta con líneas confirmadas o
 * en borrador ya no puede cambiar de código/naturaleza, ni eliminarse, ni
 * su madre volver a ser imputable automáticamente.
 */
@Service
@RequiredArgsConstructor
public class CuentaContableService {

    private static final int NIVEL_MAXIMO = 5;

    private final CuentaContableRepository repo;
    private final CuentaContableMapper mapper;
    private final AuditoriaService auditoria;
    private final RubroRepository rubroRepo;
    private final ProyectoRepository proyectoRepo;
    private final AsientoLineaRepository asientoLineaRepo;

    @Transactional(readOnly = true)
    public Page<CuentaContable> listar(String texto, Boolean activo, Pageable p) {
        return repo.buscar(texto, activo, p);
    }

    @Transactional(readOnly = true)
    public CuentaContable obtener(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Cuenta contable " + id + " no encontrada"));
    }

    @Transactional(readOnly = true)
    public List<CuentaContableNodo> arbol() {
        List<CuentaContable> todas = repo.findAllByOrderByCodigoAsc();
        Map<Long, List<CuentaContable>> hijosPorPadre = todas.stream()
                .filter(c -> c.getPadre() != null)
                .collect(Collectors.groupingBy(c -> c.getPadre().getId()));
        return todas.stream()
                .filter(c -> c.getPadre() == null)
                .map(raiz -> construirNodo(raiz, hijosPorPadre))
                .toList();
    }

    @Auditado(accion = AccionAuditoria.CREAR, entidadTipo = "CuentaContable")
    @Transactional
    public CuentaContable crear(CuentaContableCrearRequest req) {
        repo.findByCodigo(req.codigo()).ifPresent(c -> {
            throw new ConflictoException("CODIGO_DUPLICADO", "Ya existe una cuenta contable con ese código");
        });

        CuentaContable padre = req.padreId() != null ? resolverCuenta(req.padreId()) : null;
        Categoria.TipoCategoria naturaleza = Categoria.TipoCategoria.valueOf(req.naturaleza());
        CuentaContable.SaldoEsperado saldoEsperado = CuentaContable.SaldoEsperado.valueOf(req.saldoEsperado());
        Rubro rubro = req.rubroId() != null ? resolverRubro(req.rubroId()) : null;

        if (padre != null) {
            if (nivel(padre) + 1 > NIVEL_MAXIMO) {
                throw new NegocioException("PROFUNDIDAD_MAXIMA_EXCEDIDA", "El plan de cuentas admite hasta " + NIVEL_MAXIMO + " niveles");
            }
            validarNaturalezaCoincide(naturaleza, padre.getNaturaleza(), "la cuenta madre");
        }
        validarImputableYRubro(req.imputable(), rubro, naturaleza, null);

        CuentaContable c = new CuentaContable();
        c.setCodigo(req.codigo());
        c.setNombre(req.nombre());
        c.setPadre(padre);
        c.setNaturaleza(naturaleza);
        c.setRubro(rubro);
        c.setImputable(req.imputable());
        c.setSaldoEsperado(saldoEsperado);
        c.setActivo(true);
        c.setProyectosUsoHabitual(resolverProyectos(req.proyectosUsoHabitualIds()));
        CuentaContable guardada = repo.save(c);

        if (padre != null && padre.isImputable()) {
            apagarImputableDePadre(padre);
        }
        return guardada;
    }

    @Transactional
    public CuentaContable editar(Long id, CuentaContableEditarRequest req) {
        CuentaContable c = obtener(id);
        var antes = mapper.aResponse(c);

        if (!c.getCodigo().equals(req.codigo())) {
            if (tieneMovimientos(id)) {
                throw new ConflictoException("CUENTA_CON_MOVIMIENTOS", "No se puede cambiar el código: la cuenta tiene movimientos");
            }
            repo.findByCodigo(req.codigo()).ifPresent(existente -> {
                if (!existente.getId().equals(id)) {
                    throw new ConflictoException("CODIGO_DUPLICADO", "Ya existe una cuenta contable con ese código");
                }
            });
        }

        Categoria.TipoCategoria naturaleza = Categoria.TipoCategoria.valueOf(req.naturaleza());
        if (!naturaleza.equals(c.getNaturaleza()) && tieneMovimientos(id)) {
            throw new ConflictoException("CUENTA_CON_MOVIMIENTOS", "No se puede cambiar la naturaleza: la cuenta tiene movimientos");
        }

        CuentaContable nuevoPadre = req.padreId() != null ? resolverCuenta(req.padreId()) : null;
        CuentaContable padreAnterior = c.getPadre();
        boolean padreCambio = !Objects.equals(idDe(nuevoPadre), idDe(padreAnterior));

        if (padreCambio && nuevoPadre != null) {
            validarSinCiclo(c, nuevoPadre);
            int alturaSubarbol = altura(c);
            if (nivel(nuevoPadre) + 1 + alturaSubarbol > NIVEL_MAXIMO) {
                throw new NegocioException("PROFUNDIDAD_MAXIMA_EXCEDIDA", "El plan de cuentas admite hasta " + NIVEL_MAXIMO + " niveles");
            }
        }
        if (nuevoPadre != null) {
            validarNaturalezaCoincide(naturaleza, nuevoPadre.getNaturaleza(), "la cuenta madre");
        }

        Rubro rubro = req.rubroId() != null ? resolverRubro(req.rubroId()) : null;
        validarImputableYRubro(req.imputable(), rubro, naturaleza, id);

        c.setCodigo(req.codigo());
        c.setNombre(req.nombre());
        c.setPadre(nuevoPadre);
        c.setNaturaleza(naturaleza);
        c.setRubro(rubro);
        c.setImputable(req.imputable());
        c.setSaldoEsperado(CuentaContable.SaldoEsperado.valueOf(req.saldoEsperado()));
        c.setProyectosUsoHabitual(resolverProyectos(req.proyectosUsoHabitualIds()));

        if (padreCambio && nuevoPadre != null && nuevoPadre.isImputable()) {
            apagarImputableDePadre(nuevoPadre);
        }

        auditoria.registrar(AccionAuditoria.EDITAR, "CuentaContable", id, antes, mapper.aResponse(c));
        return c;
    }

    @Transactional
    public CuentaContable desactivar(Long id) {
        CuentaContable c = obtener(id);
        if (!c.isImputable() && repo.existsByPadreIdAndActivoTrue(id)) {
            throw new ConflictoException("CUENTA_CON_HIJAS_ACTIVAS", "No se puede desactivar: tiene cuentas hijas activas");
        }
        var antes = mapper.aResponse(c);
        c.setActivo(false);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "CuentaContable", id, antes, mapper.aResponse(c));
        return c;
    }

    @Transactional
    public CuentaContable activar(Long id) {
        CuentaContable c = obtener(id);
        var antes = mapper.aResponse(c);
        c.setActivo(true);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "CuentaContable", id, antes, mapper.aResponse(c));
        return c;
    }

    @Transactional
    public void eliminar(Long id) {
        CuentaContable c = obtener(id);
        if (repo.existsByPadreId(id)) {
            throw new ConflictoException("CUENTA_CON_HIJAS", "No se puede eliminar: tiene cuentas hijas");
        }
        if (tieneMovimientos(id)) {
            throw new ConflictoException("TIENE_MOVIMIENTOS_ASOCIADOS", "No se puede eliminar: la cuenta tiene movimientos. Desactivala en su lugar.");
        }
        var antes = mapper.aResponse(c);
        repo.delete(c);
        auditoria.registrar(AccionAuditoria.ELIMINAR, "CuentaContable", id, antes, null);
    }

    private CuentaContableNodo construirNodo(CuentaContable c, Map<Long, List<CuentaContable>> hijosPorPadre) {
        List<CuentaContableNodo> hijos = hijosPorPadre.getOrDefault(c.getId(), List.of()).stream()
                .map(h -> construirNodo(h, hijosPorPadre))
                .toList();
        return new CuentaContableNodo(
                c.getId(), c.getCodigo(), c.getNombre(), c.getNaturaleza().name(),
                c.getRubro() != null ? c.getRubro().getId() : null,
                c.getRubro() != null ? c.getRubro().getNombre() : null,
                c.isImputable(), c.getSaldoEsperado().name(), c.isActivo(), hijos);
    }

    private void apagarImputableDePadre(CuentaContable padre) {
        if (tieneMovimientos(padre.getId())) {
            throw new ConflictoException("CUENTA_CON_MOVIMIENTOS", "La cuenta madre tiene movimientos: no puede dejar de ser imputable automáticamente");
        }
        padre.setImputable(false);
        repo.save(padre);
    }

    private void validarImputableYRubro(boolean imputable, Rubro rubro, Categoria.TipoCategoria naturaleza, Long cuentaIdSiExiste) {
        if (imputable && cuentaIdSiExiste != null && repo.existsByPadreId(cuentaIdSiExiste)) {
            throw new NegocioException("CUENTA_CON_HIJAS", "No puede ser imputable: tiene cuentas hijas");
        }
        if (imputable && rubro == null) {
            throw new NegocioException("RUBRO_REQUERIDO_PARA_IMPUTABLE", "Las cuentas imputables requieren un rubro");
        }
        if (rubro != null) {
            validarNaturalezaCoincide(naturaleza, rubro.getCategoria().getTipo(), "el rubro");
        }
    }

    private void validarNaturalezaCoincide(Categoria.TipoCategoria naturaleza, Categoria.TipoCategoria esperada, String contra) {
        if (naturaleza.equals(esperada)) {
            return;
        }
        // Excepción "Otros Resultados": esta sección agrupa a propósito cuentas de
        // signo mixto, así que una madre (o un rubro) de categoría OTROS_RESULTADOS
        // admite hijas/cuentas de Resultado Positivo (RP) o Negativo (RN).
        if (esperada == Categoria.TipoCategoria.OTROS_RESULTADOS
                && (naturaleza == Categoria.TipoCategoria.RP || naturaleza == Categoria.TipoCategoria.RN)) {
            return;
        }
        throw new NegocioException("NATURALEZA_INCONSISTENTE", "La naturaleza debe coincidir con la de " + contra);
    }

    private void validarSinCiclo(CuentaContable cuenta, CuentaContable nuevoPadre) {
        CuentaContable actual = nuevoPadre;
        while (actual != null) {
            if (actual.getId().equals(cuenta.getId())) {
                throw new NegocioException("JERARQUIA_CICLICA", "No se puede mover la cuenta bajo sí misma o uno de sus descendientes");
            }
            actual = actual.getPadre();
        }
    }

    private int nivel(CuentaContable cuenta) {
        int nivel = 1;
        CuentaContable actual = cuenta;
        while (actual.getPadre() != null) {
            nivel++;
            actual = actual.getPadre();
        }
        return nivel;
    }

    private int altura(CuentaContable cuenta) {
        List<CuentaContable> hijos = repo.findByPadreId(cuenta.getId());
        int maxima = 0;
        for (CuentaContable hijo : hijos) {
            maxima = Math.max(maxima, 1 + altura(hijo));
        }
        return maxima;
    }

    private Long idDe(CuentaContable cuenta) {
        return cuenta != null ? cuenta.getId() : null;
    }

    private CuentaContable resolverCuenta(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Cuenta contable " + id + " no encontrada"));
    }

    private Rubro resolverRubro(Long id) {
        return rubroRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Rubro " + id + " no encontrado"));
    }

    private Set<Proyecto> resolverProyectos(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(proyectoRepo.findAllById(ids));
    }

    /** Completa el punto de extensión que F3.1 §12 dejó pendiente para F3.4. */
    private boolean tieneMovimientos(Long cuentaId) {
        return asientoLineaRepo.existsByCuentaContableId(cuentaId);
    }
}
