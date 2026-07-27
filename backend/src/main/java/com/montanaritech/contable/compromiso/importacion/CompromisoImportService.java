package com.montanaritech.contable.compromiso.importacion;

import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.importacion.ImportUtils;
import com.montanaritech.contable.compromiso.Compromiso;
import com.montanaritech.contable.compromiso.CompromisoMapper;
import com.montanaritech.contable.compromiso.CompromisoRepository;
import com.montanaritech.contable.compromiso.CompromisoService;
import com.montanaritech.contable.compromiso.TipoCompromiso;
import com.montanaritech.contable.compromiso.dto.CompromisoCrearRequest;
import com.montanaritech.contable.compromiso.dto.CompromisoResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Previsualización/confirmación de la importación de compromisos desde la
 * sección "1. Plan de Pagos Impuesto a las Ganancias" de "Presupuesto de
 * Pagos" (F10.2, molde de {@code ClienteImportService}).
 *
 * Todas las filas son {@code tipo=CUOTA_PLAN_DE_PAGOS}, sin proveedor ni
 * proyecto (obligación propia de la empresa), y generan además un
 * {@code Vencimiento} vinculado (F8.1) para aparecer en el calendario.
 */
@Service
@RequiredArgsConstructor
public class CompromisoImportService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final List<CompromisoImportParser> parsers;
    private final CompromisoRepository compromisoRepo;
    private final CompromisoService compromisoService;
    private final CompromisoMapper compromisoMapper;

    public List<CompromisoImportFilaDto> previsualizar(MultipartFile archivo) {
        String nombreArchivo = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "";
        CompromisoImportParser parser = parsers.stream()
                .filter(p -> p.soporta(nombreArchivo))
                .findFirst()
                .orElseThrow(() -> new NegocioException("FORMATO_NO_SOPORTADO", "Formato de archivo no soportado. Usá .xlsx o .csv"));

        List<CompromisoImportFilaCruda> crudas;
        try (InputStream in = archivo.getInputStream()) {
            crudas = parser.parsear(in);
        } catch (IOException e) {
            throw new NegocioException("ARCHIVO_ILEGIBLE", "No se pudo leer el archivo: " + e.getMessage());
        }

        return crudas.stream().map(this::validar).toList();
    }

    @Transactional
    public CompromisoImportResultado confirmar(Long monedaId, List<CompromisoImportFilaDto> filas) {
        List<CompromisoResponse> creadas = new ArrayList<>();
        List<CompromisoImportFilaDto> rechazadas = new ArrayList<>();
        int yaExistian = 0;

        for (CompromisoImportFilaDto fila : filas) {
            List<String> errores = revalidar(fila);
            if (!errores.isEmpty()) {
                rechazadas.add(new CompromisoImportFilaDto(fila.fila(), fila.concepto(), fila.fechaPrevista(), fila.importe(), errores));
                continue;
            }

            if (compromisoRepo.existsByConceptoAndFechaPrevista(fila.concepto(), fila.fechaPrevista())) {
                yaExistian++;
                continue;
            }

            CompromisoCrearRequest req = new CompromisoCrearRequest(
                    fila.concepto(), TipoCompromiso.CUOTA_PLAN_DE_PAGOS, fila.fechaPrevista(), fila.importe(),
                    monedaId, null, null, "Importado F10.2 (Presupuesto de Pagos §1)", true);
            Compromiso creado = compromisoService.crear(req);
            creadas.add(compromisoMapper.aResponse(creado));
        }

        return new CompromisoImportResultado(creadas, rechazadas, yaExistian);
    }

    private CompromisoImportFilaDto validar(CompromisoImportFilaCruda cruda) {
        List<String> errores = new ArrayList<>();

        String concepto = ImportUtils.vacioANull(cruda.concepto());
        if (concepto == null) {
            errores.add("El concepto es obligatorio");
        }

        var fechaPrevista = ImportUtils.parsearFecha(cruda.fechaVencimiento(), FORMATO_FECHA, "fecha de vencimiento", errores);
        if (fechaPrevista == null && ImportUtils.vacioANull(cruda.fechaVencimiento()) == null) {
            errores.add("La fecha de vencimiento es obligatoria");
        }

        var importe = ImportUtils.parsearImporte(cruda.total(), "total", errores);
        if (importe == null && ImportUtils.vacioANull(cruda.total()) == null) {
            errores.add("El total es obligatorio");
        }

        return new CompromisoImportFilaDto(cruda.numeroFila(), concepto, fechaPrevista, importe, errores);
    }

    private List<String> revalidar(CompromisoImportFilaDto fila) {
        List<String> errores = new ArrayList<>();
        if (fila.concepto() == null || fila.concepto().isBlank()) {
            errores.add("El concepto es obligatorio");
        }
        if (fila.fechaPrevista() == null) {
            errores.add("La fecha de vencimiento es obligatoria");
        }
        if (fila.importe() == null) {
            errores.add("El total es obligatorio");
        } else if (fila.importe().signum() < 0) {
            errores.add("El total no puede ser negativo");
        }
        return errores;
    }
}
