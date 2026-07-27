package com.montanaritech.contable.maestros.proveedor.importacion;

import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.importacion.ImportUtils;
import com.montanaritech.contable.maestros.proveedor.Proveedor;
import com.montanaritech.contable.maestros.proveedor.ProveedorMapper;
import com.montanaritech.contable.maestros.proveedor.ProveedorRepository;
import com.montanaritech.contable.maestros.proveedor.ProveedorService;
import com.montanaritech.contable.maestros.proveedor.dto.ProveedorCrearRequest;
import com.montanaritech.contable.maestros.proveedor.dto.ProveedorResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Previsualización/confirmación de la importación de proveedores desde la
 * hoja "Proveedores de servicios" (F10.2, molde de {@code ClienteImportService}).
 *
 * Fila sin CUIT se rechaza (decisión de F10.1). Con los datos reales de hoy,
 * ninguna de las 8 filas trae CUIT — se rechazan todas, documentado en el
 * output doc, no es una falla del importador.
 */
@Service
@RequiredArgsConstructor
public class ProveedorImportService {

    private final List<ProveedorImportParser> parsers;
    private final ProveedorRepository proveedorRepo;
    private final ProveedorService proveedorService;
    private final ProveedorMapper proveedorMapper;

    public List<ProveedorImportFilaDto> previsualizar(MultipartFile archivo) {
        String nombreArchivo = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "";
        ProveedorImportParser parser = parsers.stream()
                .filter(p -> p.soporta(nombreArchivo))
                .findFirst()
                .orElseThrow(() -> new NegocioException("FORMATO_NO_SOPORTADO", "Formato de archivo no soportado. Usá .xlsx o .csv"));

        List<ProveedorImportFilaCruda> crudas;
        try (InputStream in = archivo.getInputStream()) {
            crudas = parser.parsear(in);
        } catch (IOException e) {
            throw new NegocioException("ARCHIVO_ILEGIBLE", "No se pudo leer el archivo: " + e.getMessage());
        }

        return crudas.stream().map(this::validar).toList();
    }

    @Transactional
    public ProveedorImportResultado confirmar(Long jurisdiccionIdPorDefecto, List<ProveedorImportFilaDto> filas) {
        List<ProveedorResponse> creadas = new ArrayList<>();
        List<ProveedorImportFilaDto> rechazadas = new ArrayList<>();
        int yaExistian = 0;

        for (ProveedorImportFilaDto fila : filas) {
            List<String> errores = revalidar(fila);
            if (!errores.isEmpty()) {
                rechazadas.add(new ProveedorImportFilaDto(fila.fila(), fila.nombre(), fila.cuit(), errores));
                continue;
            }

            if (proveedorRepo.findByNombreIgnoreCase(fila.nombre()).isPresent()) {
                yaExistian++;
                continue;
            }

            ProveedorCrearRequest req = new ProveedorCrearRequest(
                    fila.nombre(), fila.cuit(), jurisdiccionIdPorDefecto, null, Set.of(), null, null, null, null, null);
            Proveedor creado = proveedorService.crear(req);
            creadas.add(proveedorMapper.aResponse(creado));
        }

        return new ProveedorImportResultado(creadas, rechazadas, yaExistian);
    }

    private ProveedorImportFilaDto validar(ProveedorImportFilaCruda cruda) {
        List<String> errores = new ArrayList<>();

        String nombre = ImportUtils.vacioANull(cruda.nombre());
        if (nombre == null) {
            errores.add("El nombre es obligatorio");
        }

        String cuit = ImportUtils.vacioANull(cruda.cuit());
        if (cuit == null) {
            errores.add("CUIT no informado — no se migra automáticamente (decisión F10.1), completar a mano después");
        }

        return new ProveedorImportFilaDto(cruda.numeroFila(), nombre, cuit, errores);
    }

    private List<String> revalidar(ProveedorImportFilaDto fila) {
        List<String> errores = new ArrayList<>();
        if (fila.nombre() == null || fila.nombre().isBlank()) {
            errores.add("El nombre es obligatorio");
        }
        if (fila.cuit() == null || fila.cuit().isBlank()) {
            errores.add("CUIT no informado — no se migra automáticamente (decisión F10.1), completar a mano después");
        }
        return errores;
    }
}
