package com.montanaritech.contable.facturacion.importacion.historica;

import com.montanaritech.contable.common.asiento.BuscarAsientoPorComprobante;
import com.montanaritech.contable.contabilidad.asiento.Asiento;
import com.montanaritech.contable.facturacion.importacion.ImportacionFacturaService;
import com.montanaritech.contable.facturacion.importacion.dto.FilaImportacionConfirmarRequest;
import com.montanaritech.contable.facturacion.importacion.dto.FilaImportacionPreviewResponse;
import com.montanaritech.contable.facturacion.importacion.dto.FilaImportacionResultadoResponse;
import com.montanaritech.contable.facturacion.importacion.historica.dto.FilaImportacionHistoricaPreviewResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * F10.3: reconstrucción histórica de facturas de venta/compra reales
 * (oct/2025→abr/2026) reusando {@link ImportacionFacturaService} de F4.6
 * tal cual para el parseo y la creación — este servicio solo agrega la
 * decisión de "¿genero un asiento nuevo, o vinculo a uno ya migrado del
 * Libro Diario en F10.2?".
 *
 * <p><b>Agujero real</b> (01/10/2025-31/12/2025, verificado en F10.1: sin
 * ningún asiento migrado): las facturas con fecha en ese rango siempre
 * generan un asiento nuevo (flujo normal de F4.6). Fuera del agujero
 * (sep/2025, ene-jul/2026) ya hay un asiento migrado para cada movimiento
 * real, así que la factura tiene que vincularse a ese asiento — nunca se
 * genera uno nuevo, para no duplicar el Libro Diario.
 *
 * <p>El matching ({@link BuscarAsientoPorComprobante}) y la decisión de
 * agujero se recalculan siempre server-side en {@link #confirmar}, nunca
 * se confía en lo que mande el cliente — mismo criterio que el resto del
 * proyecto (F10.2: revalidar en confirmar, no solo en previsualizar).
 */
@Service
@RequiredArgsConstructor
public class ImportacionFacturaHistoricaService {

    /** F10.1: único tramo sin ningún asiento migrado del Libro Diario. */
    static final LocalDate AGUJERO_DESDE = LocalDate.of(2025, 10, 1);
    static final LocalDate AGUJERO_HASTA = LocalDate.of(2025, 12, 31);

    private final ImportacionFacturaService importacionFacturaService;
    private final BuscarAsientoPorComprobante buscarAsientoPorComprobante;

    public FilaImportacionHistoricaPreviewResponse previsualizar(String nombreArchivo, byte[] bytesPdf) {
        FilaImportacionPreviewResponse base = importacionFacturaService.previsualizar(nombreArchivo, bytesPdf);
        return enriquecer(base);
    }

    private FilaImportacionHistoricaPreviewResponse enriquecer(FilaImportacionPreviewResponse base) {
        if (base.fecha() == null) {
            return new FilaImportacionHistoricaPreviewResponse(base, false, List.of(),
                    "No se pudo extraer la fecha — no se puede decidir agujero vs. vincular.");
        }
        if (enAgujero(base.fecha())) {
            return new FilaImportacionHistoricaPreviewResponse(base, true, List.of(), null);
        }
        List<Asiento> candidatos = buscarAsientoPorComprobante.buscar(base.puntoVenta(), base.numero());
        List<Long> ids = candidatos.stream().map(Asiento::getId).toList();
        String advertencia = switch (ids.size()) {
            case 0 -> "Sin asiento correspondiente en el Libro Diario — revisar a mano.";
            case 1 -> null;
            default -> ids.size() + " asientos candidatos — ambigüedad real, revisar a mano.";
        };
        return new FilaImportacionHistoricaPreviewResponse(base, false, ids, advertencia);
    }

    static boolean enAgujero(LocalDate fecha) {
        return !fecha.isBefore(AGUJERO_DESDE) && !fecha.isAfter(AGUJERO_HASTA);
    }

    /**
     * Recalcula la decisión de agujero/vinculación para cada fila (ignora
     * cualquier {@code asientoIdExistente} que venga del cliente) y delega
     * en {@link ImportacionFacturaService#confirmar} — mismo motor de F4.6,
     * cero duplicación de la lógica de alta rápida/creación de factura.
     */
    public List<FilaImportacionResultadoResponse> confirmar(List<FilaImportacionConfirmarRequest> filas) {
        List<FilaImportacionConfirmarRequest> resueltas = new ArrayList<>();
        List<FilaImportacionResultadoResponse> rechazosInmediatos = new ArrayList<>();

        for (FilaImportacionConfirmarRequest fila : filas) {
            if (fila.fecha() == null) {
                rechazosInmediatos.add(rechazo(fila, "Falta la fecha — no se puede decidir agujero vs. vincular."));
                continue;
            }
            if (enAgujero(fila.fecha())) {
                resueltas.add(conAsientoIdExistente(fila, null));
                continue;
            }
            List<Asiento> candidatos = buscarAsientoPorComprobante.buscar(fila.puntoVenta(), fila.numero());
            if (candidatos.size() == 1) {
                resueltas.add(conAsientoIdExistente(fila, candidatos.get(0).getId()));
            } else if (candidatos.isEmpty()) {
                rechazosInmediatos.add(rechazo(fila, "Sin asiento correspondiente en el Libro Diario — revisar a mano."));
            } else {
                rechazosInmediatos.add(rechazo(fila,
                        candidatos.size() + " asientos candidatos — ambigüedad real, revisar a mano."));
            }
        }

        List<FilaImportacionResultadoResponse> resultados = new ArrayList<>(rechazosInmediatos);
        resultados.addAll(importacionFacturaService.confirmar(resueltas));
        return resultados;
    }

    private FilaImportacionConfirmarRequest conAsientoIdExistente(FilaImportacionConfirmarRequest fila, Long asientoId) {
        return new FilaImportacionConfirmarRequest(fila.nombreArchivo(), fila.tipo(), fila.clienteId(), fila.proveedorId(),
                fila.altaRapidaNombre(), fila.altaRapidaCuit(), fila.altaRapidaJurisdiccionId(), fila.proyectoId(),
                fila.fecha(), fila.fechaVencimiento(), fila.tipoComprobante(), fila.puntoVenta(), fila.numero(),
                fila.monedaId(), fila.tipoCambio(), fila.observaciones(), fila.descripcionLinea(), fila.importeNeto(),
                fila.alicuotaIva(), fila.tipoIngreso(), fila.tipoCostoId(), fila.estadoDestino(), asientoId);
    }

    private FilaImportacionResultadoResponse rechazo(FilaImportacionConfirmarRequest fila, String motivo) {
        return new FilaImportacionResultadoResponse(fila.nombreArchivo(), false, fila.tipo(), fila.numero(),
                null, null, null, motivo, null);
    }
}
