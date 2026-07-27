package com.montanaritech.contable.maestros.proyecto.comision.importacion;

import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.importacion.ImportUtils;
import com.montanaritech.contable.maestros.comisionista.Comisionista;
import com.montanaritech.contable.maestros.comisionista.ComisionistaRepository;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import com.montanaritech.contable.maestros.proyecto.comision.ComisionProyecto;
import com.montanaritech.contable.maestros.proyecto.comision.ComisionProyectoMapper;
import com.montanaritech.contable.maestros.proyecto.comision.ComisionProyectoRepository;
import com.montanaritech.contable.maestros.proyecto.comision.ComisionProyectoService;
import com.montanaritech.contable.maestros.proyecto.comision.dto.ComisionProyectoCrearRequest;
import com.montanaritech.contable.maestros.proyecto.comision.dto.ComisionProyectoEditarRequest;
import com.montanaritech.contable.maestros.proyecto.comision.dto.ComisionProyectoResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Previsualización/confirmación de la importación de comisiones desde la
 * hoja "Comisiones por ventas" (F10.2, molde de {@code ProyectoImportService}).
 *
 * Filas sin {@code %} (12 de 14, decisión de F10.1 aprobada por el equipo):
 * se calcula un % sintético (monto ÷ Proyecto.montoTotal × 100) para poder
 * crear la comisión, y en el mismo paso se llama {@code editar()} para fijar
 * {@code importeFinal} al monto real de la hoja — los datos reales quedan
 * correctos aunque el % guardado sea artificial.
 */
@Service
@RequiredArgsConstructor
public class ComisionProyectoImportService {

    private final List<ComisionProyectoImportParser> parsers;
    private final ProyectoRepository proyectoRepo;
    private final ComisionistaRepository comisionistaRepo;
    private final ComisionProyectoRepository comisionProyectoRepo;
    private final ComisionProyectoService comisionProyectoService;
    private final ComisionProyectoMapper comisionProyectoMapper;

    public List<ComisionProyectoImportFilaDto> previsualizar(MultipartFile archivo) {
        String nombreArchivo = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "";
        ComisionProyectoImportParser parser = parsers.stream()
                .filter(p -> p.soporta(nombreArchivo))
                .findFirst()
                .orElseThrow(() -> new NegocioException("FORMATO_NO_SOPORTADO", "Formato de archivo no soportado. Usá .xlsx o .csv"));

        List<ComisionProyectoImportFilaCruda> crudas;
        try (InputStream in = archivo.getInputStream()) {
            crudas = parser.parsear(in);
        } catch (IOException e) {
            throw new NegocioException("ARCHIVO_ILEGIBLE", "No se pudo leer el archivo: " + e.getMessage());
        }

        return crudas.stream().map(this::validar).toList();
    }

    @Transactional
    public ComisionProyectoImportResultado confirmar(Long monedaIdArs, Long monedaIdUsd, List<ComisionProyectoImportFilaDto> filas) {
        List<ComisionProyectoResponse> creadas = new ArrayList<>();
        List<ComisionProyectoImportFilaDto> rechazadas = new ArrayList<>();
        int yaExistian = 0;

        for (ComisionProyectoImportFilaDto fila : filas) {
            List<String> errores = revalidar(fila);
            var proyectoOpt = errores.isEmpty() ? proyectoRepo.findByNombreIgnoreCase(fila.proyectoNombre()) : java.util.Optional.<Proyecto>empty();
            var comisionistaOpt = errores.isEmpty() ? comisionistaRepo.findByNombreIgnoreCase(fila.comisionistaNombre()) : java.util.Optional.<Comisionista>empty();
            if (errores.isEmpty() && proyectoOpt.isEmpty()) {
                errores.add("Proyecto '" + fila.proyectoNombre() + "' no encontrado");
            }
            if (errores.isEmpty() && comisionistaOpt.isEmpty()) {
                errores.add("Comisionista '" + fila.comisionistaNombre() + "' no encontrado — darlo de alta a mano antes de reintentar");
            }
            BigDecimal porcentaje = fila.porcentaje();
            if (errores.isEmpty() && porcentaje == null) {
                Proyecto proyecto = proyectoOpt.get();
                if (proyecto.getMontoTotal() == null || proyecto.getMontoTotal().signum() == 0) {
                    errores.add("Proyecto sin monto total: no se puede calcular un % sintético");
                } else {
                    porcentaje = fila.monto().multiply(BigDecimal.valueOf(100))
                            .divide(proyecto.getMontoTotal(), 2, RoundingMode.HALF_UP);
                    if (porcentaje.compareTo(BigDecimal.valueOf(100)) > 0) {
                        porcentaje = BigDecimal.valueOf(100);
                    }
                }
            }

            if (!errores.isEmpty()) {
                rechazadas.add(new ComisionProyectoImportFilaDto(fila.fila(), fila.proyectoNombre(),
                        fila.comisionistaNombre(), fila.porcentaje(), fila.monto(), fila.esUsd(), errores));
                continue;
            }

            Long proyectoId = proyectoOpt.get().getId();
            Long comisionistaId = comisionistaOpt.get().getId();
            if (comisionProyectoRepo.existsByProyectoIdAndComisionistaId(proyectoId, comisionistaId)) {
                yaExistian++;
                continue;
            }

            Long monedaId = fila.esUsd() ? monedaIdUsd : monedaIdArs;
            boolean porcentajeSintetico = fila.porcentaje() == null;
            ComisionProyectoCrearRequest req = new ComisionProyectoCrearRequest(
                    comisionistaId, porcentaje, "MONTO_TOTAL", monedaId, null,
                    "Importado F10.2" + (porcentajeSintetico ? " (% sintético, ver importeFinal)" : ""));
            ComisionProyecto creada = comisionProyectoService.crear(proyectoId, req);

            if (porcentajeSintetico) {
                ComisionProyectoEditarRequest editReq = new ComisionProyectoEditarRequest(
                        comisionistaId, porcentaje, "MONTO_TOTAL", monedaId, fila.monto(), null, null,
                        creada.getObservaciones());
                creada = comisionProyectoService.editar(proyectoId, creada.getId(), editReq);
            }
            creadas.add(comisionProyectoMapper.aResponse(creada));
        }

        return new ComisionProyectoImportResultado(creadas, rechazadas, yaExistian);
    }

    private ComisionProyectoImportFilaDto validar(ComisionProyectoImportFilaCruda cruda) {
        List<String> errores = new ArrayList<>();

        String proyecto = ImportUtils.vacioANull(cruda.proyecto());
        if (proyecto == null) {
            errores.add("El Proyecto es obligatorio");
        } else if (proyectoRepo.findByNombreIgnoreCase(proyecto).isEmpty()) {
            errores.add("Proyecto '" + proyecto + "' no encontrado");
        }

        String comisionista = ImportUtils.vacioANull(cruda.comisionista());
        if (comisionista == null) {
            errores.add("El Comisionista es obligatorio");
        } else if (comisionistaRepo.findByNombreIgnoreCase(comisionista).isEmpty()) {
            errores.add("Comisionista '" + comisionista + "' no encontrado — darlo de alta a mano antes de reintentar");
        }

        BigDecimal porcentaje = ImportUtils.parsearImporte(cruda.porcentaje(), "porcentaje", errores);
        BigDecimal monto = ImportUtils.parsearImporte(cruda.monto(), "monto", errores);
        if (monto == null) {
            errores.add("El monto de la comisión es obligatorio");
        }

        String comentarios = ImportUtils.vacioANull(cruda.comentarios());
        boolean esUsd = comentarios != null && comentarios.toLowerCase().contains("dol");

        return new ComisionProyectoImportFilaDto(cruda.numeroFila(), proyecto, comisionista, porcentaje, monto, esUsd, errores);
    }

    private List<String> revalidar(ComisionProyectoImportFilaDto fila) {
        List<String> errores = new ArrayList<>();
        if (fila.proyectoNombre() == null) {
            errores.add("El Proyecto es obligatorio");
        }
        if (fila.comisionistaNombre() == null) {
            errores.add("El Comisionista es obligatorio");
        }
        if (fila.monto() == null) {
            errores.add("El monto de la comisión es obligatorio");
        } else if (fila.monto().signum() < 0) {
            errores.add("El monto no puede ser negativo");
        }
        return errores;
    }
}
