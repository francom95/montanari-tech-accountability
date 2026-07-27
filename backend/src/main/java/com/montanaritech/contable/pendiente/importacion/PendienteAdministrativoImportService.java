package com.montanaritech.contable.pendiente.importacion;

import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.importacion.ImportUtils;
import com.montanaritech.contable.pendiente.PendienteAdministrativo;
import com.montanaritech.contable.pendiente.PendienteAdministrativoMapper;
import com.montanaritech.contable.pendiente.PendienteAdministrativoRepository;
import com.montanaritech.contable.pendiente.PendienteAdministrativoService;
import com.montanaritech.contable.pendiente.PrioridadPendiente;
import com.montanaritech.contable.pendiente.dto.PendienteAdministrativoCrearRequest;
import com.montanaritech.contable.pendiente.dto.PendienteAdministrativoResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Previsualización/confirmación de la importación de pendientes
 * administrativos desde "PENDIENTES"/"PENDIENTES AHORA" (F10.2, molde de
 * {@code ClienteImportService}) — la carga más directa de F10.2, sin
 * transformación (F10.1 §14): {@code categoria=null}, {@code fechaEstimadaResolucion=null},
 * {@code prioridad=MEDIA}.
 */
@Service
@RequiredArgsConstructor
public class PendienteAdministrativoImportService {

    private final List<PendienteAdministrativoImportParser> parsers;
    private final PendienteAdministrativoRepository pendienteRepo;
    private final PendienteAdministrativoService pendienteService;
    private final PendienteAdministrativoMapper pendienteMapper;

    public List<PendienteAdministrativoImportFilaDto> previsualizar(MultipartFile archivo) {
        String nombreArchivo = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "";
        PendienteAdministrativoImportParser parser = parsers.stream()
                .filter(p -> p.soporta(nombreArchivo))
                .findFirst()
                .orElseThrow(() -> new NegocioException("FORMATO_NO_SOPORTADO", "Formato de archivo no soportado. Usá .xlsx o .csv"));

        List<PendienteAdministrativoImportFilaCruda> crudas;
        try (InputStream in = archivo.getInputStream()) {
            crudas = parser.parsear(in);
        } catch (IOException e) {
            throw new NegocioException("ARCHIVO_ILEGIBLE", "No se pudo leer el archivo: " + e.getMessage());
        }

        return crudas.stream().map(this::validar).toList();
    }

    @Transactional
    public PendienteAdministrativoImportResultado confirmar(List<PendienteAdministrativoImportFilaDto> filas) {
        List<PendienteAdministrativoResponse> creadas = new ArrayList<>();
        List<PendienteAdministrativoImportFilaDto> rechazadas = new ArrayList<>();
        int yaExistian = 0;

        for (PendienteAdministrativoImportFilaDto fila : filas) {
            List<String> errores = revalidar(fila);
            if (!errores.isEmpty()) {
                rechazadas.add(new PendienteAdministrativoImportFilaDto(fila.fila(), fila.titulo(), errores));
                continue;
            }

            if (pendienteRepo.findByTitulo(fila.titulo()).isPresent()) {
                yaExistian++;
                continue;
            }

            PendienteAdministrativoCrearRequest req = new PendienteAdministrativoCrearRequest(
                    fila.titulo(), null, null, PrioridadPendiente.MEDIA, null, null, null, null, null, "Importado F10.2");
            PendienteAdministrativo creado = pendienteService.crear(req);
            creadas.add(pendienteMapper.aResponse(creado));
        }

        return new PendienteAdministrativoImportResultado(creadas, rechazadas, yaExistian);
    }

    private PendienteAdministrativoImportFilaDto validar(PendienteAdministrativoImportFilaCruda cruda) {
        List<String> errores = new ArrayList<>();
        String titulo = ImportUtils.vacioANull(cruda.titulo());
        if (titulo == null) {
            errores.add("El título es obligatorio");
        }
        return new PendienteAdministrativoImportFilaDto(cruda.numeroFila(), titulo, errores);
    }

    private List<String> revalidar(PendienteAdministrativoImportFilaDto fila) {
        List<String> errores = new ArrayList<>();
        if (fila.titulo() == null || fila.titulo().isBlank()) {
            errores.add("El título es obligatorio");
        }
        return errores;
    }
}
