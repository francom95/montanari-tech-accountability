package com.montanaritech.contable.inversion.importacion;

import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.importacion.ImportUtils;
import com.montanaritech.contable.inversion.Inversion;
import com.montanaritech.contable.inversion.InversionRepository;
import com.montanaritech.contable.inversion.InversionService;
import com.montanaritech.contable.inversion.MovimientoInversion;
import com.montanaritech.contable.inversion.MovimientoInversionRepository;
import com.montanaritech.contable.inversion.MovimientoInversionService;
import com.montanaritech.contable.inversion.TipoMovimientoInversion;
import com.montanaritech.contable.inversion.dto.InversionCrearRequest;
import com.montanaritech.contable.inversion.dto.MovimientoInversionCrearRequest;
import com.montanaritech.contable.inversion.dto.MovimientoInversionResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Previsualización/confirmación de la importación de movimientos de
 * inversión desde "Inversiones en Fondos Fima" (F10.2, molde de
 * {@code ClienteImportService}).
 *
 * Filas con {@code Operación} vacía ("Valuación del Fondo Fima") son
 * revaluaciones sin movimiento real — se descartan en {@code previsualizar}
 * antes de llegar a validar, sin aparecer en creadas ni en rechazadas
 * (decisión de F10.1: el sistema recalcula la valuación sola desde el
 * último {@code valorCuotaparte} cargado).
 *
 * {@code cuentaOrigenId} es parámetro de la confirmación: la hoja no trae la
 * cuenta bancaria de origen. {@code vinculoTipo}/{@code vinculoRefId} de la
 * {@code Inversion} quedan siempre {@code null} — cruzar la columna
 * "Detalle" contra Compromiso/Vencimiento es frágil, se deja para vincular
 * a mano después vía la UI ya existente de F8.4.
 */
@Service
@RequiredArgsConstructor
public class InversionImportService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final List<InversionImportParser> parsers;
    private final InversionRepository inversionRepo;
    private final InversionService inversionService;
    private final MovimientoInversionRepository movimientoInversionRepo;
    private final MovimientoInversionService movimientoInversionService;

    public List<InversionImportFilaDto> previsualizar(MultipartFile archivo) {
        String nombreArchivo = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "";
        InversionImportParser parser = parsers.stream()
                .filter(p -> p.soporta(nombreArchivo))
                .findFirst()
                .orElseThrow(() -> new NegocioException("FORMATO_NO_SOPORTADO", "Formato de archivo no soportado. Usá .xlsx o .csv"));

        List<InversionImportFilaCruda> crudas;
        try (InputStream in = archivo.getInputStream()) {
            crudas = parser.parsear(in);
        } catch (IOException e) {
            throw new NegocioException("ARCHIVO_ILEGIBLE", "No se pudo leer el archivo: " + e.getMessage());
        }

        return crudas.stream()
                .filter(c -> ImportUtils.vacioANull(c.operacion()) != null)
                .map(this::validar)
                .toList();
    }

    @Transactional
    public InversionImportResultado confirmar(Long cuentaOrigenId, List<InversionImportFilaDto> filas) {
        List<MovimientoInversionResponse> creadas = new ArrayList<>();
        List<InversionImportFilaDto> rechazadas = new ArrayList<>();
        int yaExistian = 0;
        Map<String, Long> inversionesResueltas = new HashMap<>();

        for (InversionImportFilaDto fila : filas) {
            List<String> errores = revalidar(fila);
            if (!errores.isEmpty()) {
                rechazadas.add(new InversionImportFilaDto(fila.fila(), fila.instrumento(), fila.objetivoDelDinero(),
                        fila.tipo(), fila.fecha(), fila.cuotapartes(), fila.valorCuotaparte(), fila.montoAplicado(), errores));
                continue;
            }

            Long inversionId = inversionesResueltas.computeIfAbsent(fila.instrumento().toLowerCase(),
                    k -> resolverOCrearInversion(fila.instrumento(), fila.objetivoDelDinero(), cuentaOrigenId));

            if (movimientoInversionRepo.existsByInversion_IdAndFechaAndTipoAndCuotapartes(
                    inversionId, fila.fecha(), fila.tipo(), fila.cuotapartes())) {
                yaExistian++;
                continue;
            }

            MovimientoInversionCrearRequest req = new MovimientoInversionCrearRequest(
                    inversionId, fila.tipo(), fila.fecha(), fila.montoAplicado(), fila.cuotapartes(),
                    fila.valorCuotaparte(), fila.fecha(), "Importado F10.2");
            MovimientoInversion creado = movimientoInversionService.crear(req);
            creadas.add(movimientoInversionService.aResponse(creado));
        }

        return new InversionImportResultado(creadas, rechazadas, yaExistian);
    }

    private Long resolverOCrearInversion(String instrumento, String objetivoDelDinero, Long cuentaOrigenId) {
        return inversionRepo.findByInstrumentoIgnoreCase(instrumento)
                .map(Inversion::getId)
                .orElseGet(() -> {
                    InversionCrearRequest req = new InversionCrearRequest(instrumento, cuentaOrigenId, objetivoDelDinero, null, null);
                    return inversionService.crear(req).getId();
                });
    }

    private InversionImportFilaDto validar(InversionImportFilaCruda cruda) {
        List<String> errores = new ArrayList<>();

        String instrumento = ImportUtils.vacioANull(cruda.fondo());
        if (instrumento == null) {
            errores.add("El Fondo es obligatorio");
        }

        String operacionCruda = ImportUtils.vacioANull(cruda.operacion());
        TipoMovimientoInversion tipo = null;
        if (operacionCruda == null) {
            errores.add("La Operación es obligatoria");
        } else if (operacionCruda.equalsIgnoreCase("Agregar")) {
            tipo = TipoMovimientoInversion.SUSCRIPCION;
        } else if (operacionCruda.equalsIgnoreCase("Retirar")) {
            tipo = TipoMovimientoInversion.RESCATE;
        } else {
            errores.add("Operación desconocida (esperado 'Agregar' o 'Retirar'): " + operacionCruda);
        }

        var fecha = ImportUtils.parsearFecha(cruda.fechaLiquidacion(), FORMATO_FECHA, "fecha de liquidación", errores);
        if (fecha == null && ImportUtils.vacioANull(cruda.fechaLiquidacion()) == null) {
            errores.add("La fecha de liquidación es obligatoria");
        }

        var cuotapartes = ImportUtils.parsearImporte(cruda.cuotapartes(), "cuotapartes", errores);
        if (cuotapartes == null && ImportUtils.vacioANull(cruda.cuotapartes()) == null) {
            errores.add("Las cuotapartes son obligatorias");
        }

        var valorCuotaparte = ImportUtils.parsearImporte(cruda.valorCuotaparte(), "valor cuotaparte", errores);
        if (valorCuotaparte == null && ImportUtils.vacioANull(cruda.valorCuotaparte()) == null) {
            errores.add("El valor cuotaparte es obligatorio");
        }

        var monto = ImportUtils.parsearImporte(cruda.monto(), "monto", errores);
        if (monto == null && ImportUtils.vacioANull(cruda.monto()) == null) {
            errores.add("El monto es obligatorio");
        }

        return new InversionImportFilaDto(cruda.numeroFila(), instrumento, ImportUtils.vacioANull(cruda.detalle()),
                tipo, fecha, cuotapartes, valorCuotaparte, monto, errores);
    }

    private List<String> revalidar(InversionImportFilaDto fila) {
        List<String> errores = new ArrayList<>();
        if (fila.instrumento() == null) {
            errores.add("El Fondo es obligatorio");
        }
        if (fila.tipo() == null) {
            errores.add("La Operación es obligatoria");
        }
        if (fila.fecha() == null) {
            errores.add("La fecha de liquidación es obligatoria");
        }
        if (fila.cuotapartes() == null || fila.cuotapartes().signum() <= 0) {
            errores.add("Las cuotapartes deben ser positivas");
        }
        if (fila.valorCuotaparte() == null || fila.valorCuotaparte().signum() <= 0) {
            errores.add("El valor cuotaparte debe ser positivo");
        }
        if (fila.montoAplicado() == null || fila.montoAplicado().signum() <= 0) {
            errores.add("El monto debe ser positivo");
        }
        return errores;
    }
}
