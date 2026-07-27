package com.montanaritech.contable.common.asiento;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.contabilidad.asiento.Asiento;
import com.montanaritech.contable.contabilidad.asiento.AsientoLineaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * F10.3: matching determinístico entre un comprobante real (factura de
 * venta/compra reconstruida desde PDF, o un cobro/pago desde un resumen
 * bancario) y un {@code Asiento} ya CONFIRMADO que ya lo registró — típico
 * de los asientos migrados del Libro Diario en F10.2, cuya leyenda embebe
 * el comprobante de origen con el patrón {@code "FC: PPPPP-NNNNNNNN"}
 * (verificado en F10.2: ~40% de las líneas migradas lo traen).
 *
 * <p>No hace falta tolerancia de fecha ni de importe acá: el patrón de
 * comprobante ya es una clave lo bastante específica (punto de venta +
 * número), así que 0 candidatos significa "no migrado todavía" (agujero
 * real o dato de origen distinto) y ≥2 significa una ambigüedad real que
 * el caller debe resolver a mano — nunca se adivina cuál usar.
 */
@Service
@RequiredArgsConstructor
public class BuscarAsientoPorComprobante {

    private final AsientoLineaRepository asientoLineaRepo;

    @Transactional(readOnly = true)
    public List<Asiento> buscar(String puntoVenta, String numero) {
        String patron = "FC: " + normalizar(puntoVenta, 5) + "-" + normalizar(numero, 8);
        return asientoLineaRepo.buscarAsientosPorLeyendaConteniendo(patron, EstadoDocumento.CONFIRMADO);
    }

    /** Deja solo dígitos y completa con ceros a la izquierda (mismo formato que el Libro Diario real). */
    private String normalizar(String valor, int longitud) {
        String soloDigitos = valor == null ? "" : valor.replaceAll("[^0-9]", "");
        if (soloDigitos.length() >= longitud) {
            return soloDigitos;
        }
        return "0".repeat(longitud - soloDigitos.length()) + soloDigitos;
    }
}
