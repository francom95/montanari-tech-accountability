package com.montanaritech.contable.contabilidad.mayor;

import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.contabilidad.asiento.AsientoLinea;
import com.montanaritech.contable.contabilidad.asiento.AsientoLineaRepository;
import com.montanaritech.contable.contabilidad.asiento.OrigenAsiento;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContableRepository;
import com.montanaritech.contable.contabilidad.mayor.dto.MayorFilaResponse;
import com.montanaritech.contable.contabilidad.mayor.dto.MayorResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mayor contable (F3.4 §5, implementado en F3.6). Todo saldo se deriva por
 * consulta sobre {@code asiento_linea} de asientos {@code CONFIRMADO}
 * (ADR-04): no hay columnas de saldo persistidas. El cálculo completo
 * (saldo anterior + acumulado línea a línea) se hace una sola vez por
 * pedido y se reutiliza tanto para la pantalla (paginada en memoria) como
 * para la exportación (streaming completo) — evita recalcular el
 * acumulado dos veces y garantiza que pantalla y export siempre muestren
 * el mismo saldo final.
 */
@Service
@RequiredArgsConstructor
public class MayorService {

    private final AsientoLineaRepository lineaRepo;
    private final CuentaContableRepository cuentaRepo;

    /** Resultado completo (sin paginar): lo usan tanto {@link #consultar} como el export del controller. */
    public record MayorCompleto(
            CuentaContable cuenta,
            boolean vistaAnalitica,
            List<MayorFilaResponse> filas,
            BigDecimal saldoFinal,
            String saldoFinalEtiqueta,
            Boolean contrarioAlEsperado
    ) {}

    @Transactional(readOnly = true)
    public MayorCompleto calcular(
            Long cuentaId, Long rubroId, Long proyectoId, Long clienteId, Long proveedorId,
            OrigenAsiento origen, Long monedaId, LocalDate fechaDesde, LocalDate fechaHasta) {
        CuentaContable cuenta = cuentaRepo.findById(cuentaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta contable " + cuentaId + " no encontrada"));
        Set<Long> cuentaIds = resolverDescendientesImputables(cuenta);
        boolean vistaAnalitica = rubroId != null || proyectoId != null || clienteId != null
                || proveedorId != null || origen != null || monedaId != null;

        if (cuentaIds.isEmpty()) {
            // Madre sin ninguna imputable descendiente todavía: mayor vacío, nada que sumar.
            return new MayorCompleto(cuenta, vistaAnalitica, List.of(), BigDecimal.ZERO, "SALDADA", null);
        }

        BigDecimal saldoAnterior = BigDecimal.ZERO;
        List<MayorFilaResponse> filas = new ArrayList<>();
        if (fechaDesde != null) {
            BigDecimal sumaDebe = lineaRepo.sumarDebeAntesDeFecha(cuentaIds, rubroId, proyectoId, clienteId,
                    proveedorId, origen, monedaId, fechaDesde, EstadoDocumento.CONFIRMADO);
            BigDecimal sumaHaber = lineaRepo.sumarHaberAntesDeFecha(cuentaIds, rubroId, proyectoId, clienteId,
                    proveedorId, origen, monedaId, fechaDesde, EstadoDocumento.CONFIRMADO);
            saldoAnterior = sumaDebe.subtract(sumaHaber);
        }

        List<AsientoLinea> lineas = lineaRepo.buscarParaMayor(cuentaIds, rubroId, proyectoId, clienteId,
                proveedorId, origen, monedaId, fechaDesde, fechaHasta, EstadoDocumento.CONFIRMADO);

        BigDecimal acumulado = saldoAnterior;
        if (fechaDesde != null) {
            filas.add(new MayorFilaResponse(true, fechaDesde, null, null, "Saldo anterior", null, null, null,
                    null, null, saldoAnterior, null, null, null, null, null));
        }
        for (AsientoLinea l : lineas) {
            acumulado = acumulado.add(l.getDebe()).subtract(l.getHaber());
            String descripcion = (l.getLeyenda() != null && !l.getLeyenda().isBlank())
                    ? l.getLeyenda() : l.getAsiento().getDescripcion();
            filas.add(new MayorFilaResponse(false, l.getAsiento().getFecha(), l.getAsiento().getId(),
                    l.getAsiento().getNumero(), descripcion, l.getCuentaContable().getId(),
                    l.getCuentaContable().getCodigo(), l.getCuentaContable().getNombre(), l.getDebe(), l.getHaber(),
                    acumulado, l.getMoneda().getId(), l.getMoneda().getCodigo(), l.getImporteOriginal(),
                    l.getTipoCambio(), l.getAsiento().getOrigen().name()));
        }

        String etiqueta = acumulado.compareTo(BigDecimal.ZERO) > 0 ? "DEUDOR"
                : acumulado.compareTo(BigDecimal.ZERO) < 0 ? "ACREEDOR" : "SALDADA";
        Boolean contrarioAlEsperado = null;
        if (!vistaAnalitica) {
            boolean esDeudor = acumulado.compareTo(BigDecimal.ZERO) > 0;
            boolean esAcreedor = acumulado.compareTo(BigDecimal.ZERO) < 0;
            contrarioAlEsperado = (cuenta.getSaldoEsperado() == CuentaContable.SaldoEsperado.DEUDOR && esAcreedor)
                    || (cuenta.getSaldoEsperado() == CuentaContable.SaldoEsperado.ACREEDOR && esDeudor);
        }

        return new MayorCompleto(cuenta, vistaAnalitica, filas, acumulado, etiqueta, contrarioAlEsperado);
    }

    public MayorResponse paginar(MayorCompleto completo, int page, int size) {
        int desde = Math.min(page * size, completo.filas().size());
        int hasta = Math.min(desde + size, completo.filas().size());
        List<MayorFilaResponse> pagina = completo.filas().subList(desde, hasta);
        int totalPaginas = (int) Math.ceil(completo.filas().size() / (double) size);

        return new MayorResponse(
                completo.cuenta().getId(), completo.cuenta().getCodigo(), completo.cuenta().getNombre(),
                !completo.cuenta().isImputable(), completo.vistaAnalitica(), pagina, page, size,
                completo.filas().size(), Math.max(totalPaginas, 1),
                completo.saldoFinal(), completo.saldoFinalEtiqueta(), completo.contrarioAlEsperado());
    }

    /**
     * Cuentas imputables a incluir: la propia cuenta si ya es imputable, o
     * todas sus imputables descendientes si es madre (F3.1 §5.3). Recorrido
     * en Java (no CTE recursivo en SQL): el árbol de cuentas de esta empresa
     * tiene un puñado de niveles y decenas de cuentas (F3.3), así que N
     * consultas cortas por {@code padre_id} es más simple y perfectamente
     * suficiente a esta escala.
     */
    private Set<Long> resolverDescendientesImputables(CuentaContable cuenta) {
        if (cuenta.isImputable()) {
            return Set.of(cuenta.getId());
        }
        Set<Long> imputables = new HashSet<>();
        Deque<CuentaContable> pendientes = new ArrayDeque<>(cuentaRepo.findByPadreId(cuenta.getId()));
        while (!pendientes.isEmpty()) {
            CuentaContable actual = pendientes.poll();
            if (actual.isImputable()) {
                imputables.add(actual.getId());
            } else {
                pendientes.addAll(cuentaRepo.findByPadreId(actual.getId()));
            }
        }
        return imputables;
    }
}
