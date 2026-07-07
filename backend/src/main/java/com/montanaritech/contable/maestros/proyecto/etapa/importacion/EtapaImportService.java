package com.montanaritech.contable.maestros.proyecto.etapa.importacion;

import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.maestros.proveedor.Proveedor;
import com.montanaritech.contable.maestros.proveedor.ProveedorRepository;
import com.montanaritech.contable.maestros.proyecto.etapa.Etapa;
import com.montanaritech.contable.maestros.proyecto.etapa.EtapaMapper;
import com.montanaritech.contable.maestros.proyecto.etapa.EtapaService;
import com.montanaritech.contable.maestros.proyecto.etapa.dto.EtapaCrearRequest;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Previsualización (parsear + validar sin persistir) y confirmación
 * (persistir solo las filas sin errores) de la importación de etapas desde
 * Excel/CSV (F2.5).
 */
@Service
@RequiredArgsConstructor
public class EtapaImportService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final List<EtapaImportParser> parsers;
    private final ProveedorRepository proveedorRepo;
    private final EtapaService etapaService;
    private final EtapaMapper etapaMapper;
    private final AuditoriaService auditoria;

    public List<EtapaImportFilaDto> previsualizar(MultipartFile archivo) {
        String nombreArchivo = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "";
        EtapaImportParser parser = parsers.stream()
                .filter(p -> p.soporta(nombreArchivo))
                .findFirst()
                .orElseThrow(() -> new NegocioException("FORMATO_NO_SOPORTADO", "Formato de archivo no soportado. Usá .xlsx o .csv"));

        List<EtapaImportFilaCruda> crudas;
        try (InputStream in = archivo.getInputStream()) {
            crudas = parser.parsear(in);
        } catch (IOException e) {
            throw new NegocioException("ARCHIVO_ILEGIBLE", "No se pudo leer el archivo: " + e.getMessage());
        }

        return crudas.stream().map(this::validar).toList();
    }

    @Transactional
    public EtapaImportResultado confirmar(Long proyectoId, List<EtapaImportFilaDto> filas) {
        List<com.montanaritech.contable.maestros.proyecto.etapa.dto.EtapaResponse> creadas = new ArrayList<>();
        List<EtapaImportFilaDto> rechazadas = new ArrayList<>();

        for (EtapaImportFilaDto fila : filas) {
            List<String> errores = revalidar(fila);
            if (!errores.isEmpty()) {
                rechazadas.add(new EtapaImportFilaDto(
                        fila.fila(), fila.nombre(), fila.descripcion(), fila.estado(), fila.fechaInicio(),
                        fila.fechaEstimadaFin(), fila.porcentajeAvance(), fila.montoPresupuestado(),
                        fila.costosEstimados(), fila.pagosPrevistos(), fila.cobrosPrevistos(), fila.observaciones(),
                        fila.proveedoresNombres(), fila.proveedoresIds(), errores));
                continue;
            }

            EtapaCrearRequest req = new EtapaCrearRequest(
                    fila.nombre(),
                    fila.descripcion(),
                    fila.estado(),
                    fila.fechaInicio(),
                    fila.fechaEstimadaFin(),
                    fila.porcentajeAvance(),
                    fila.montoPresupuestado(),
                    fila.costosEstimados(),
                    fila.proveedoresIds() == null ? Set.of() : new HashSet<>(fila.proveedoresIds()),
                    fila.pagosPrevistos(),
                    fila.cobrosPrevistos(),
                    fila.observaciones());

            Etapa creada = etapaService.crear(proyectoId, req);
            creadas.add(etapaMapper.aResponse(creada));
        }

        auditoria.registrar(AccionAuditoria.IMPORTAR, "Etapa", proyectoId,
                null, java.util.Map.of("creadas", creadas.size(), "rechazadas", rechazadas.size()));

        return new EtapaImportResultado(creadas, rechazadas);
    }

    private EtapaImportFilaDto validar(EtapaImportFilaCruda cruda) {
        List<String> errores = new ArrayList<>();

        String nombre = vacioANull(cruda.nombre());
        if (nombre == null) {
            errores.add("El nombre es obligatorio");
        }

        String estado = normalizarEstado(cruda.estado(), errores);
        LocalDate fechaInicio = parsearFecha(cruda.fechaInicio(), "fecha de inicio", errores);
        LocalDate fechaEstimadaFin = parsearFecha(cruda.fechaEstimadaFin(), "fecha estimada de fin", errores);
        Integer porcentajeAvance = parsearPorcentaje(cruda.porcentajeAvance(), errores);
        BigDecimal montoPresupuestado = parsearImporte(cruda.montoPresupuestado(), "monto presupuestado", errores);
        BigDecimal costosEstimados = parsearImporte(cruda.costosEstimados(), "costos estimados", errores);
        BigDecimal pagosPrevistos = parsearImporte(cruda.pagosPrevistos(), "pagos previstos", errores);
        BigDecimal cobrosPrevistos = parsearImporte(cruda.cobrosPrevistos(), "cobros previstos", errores);

        List<String> nombresProveedores = new ArrayList<>();
        List<Long> idsProveedores = new ArrayList<>();
        if (cruda.proveedores() != null && !cruda.proveedores().isBlank()) {
            for (String nombreProveedor : cruda.proveedores().split(";")) {
                String limpio = nombreProveedor.trim();
                if (limpio.isEmpty()) {
                    continue;
                }
                nombresProveedores.add(limpio);
                proveedorRepo.findByNombreIgnoreCase(limpio)
                        .ifPresentOrElse(
                                p -> idsProveedores.add(p.getId()),
                                () -> errores.add("Proveedor '" + limpio + "' no encontrado"));
            }
        }

        return new EtapaImportFilaDto(
                cruda.numeroFila(), nombre, vacioANull(cruda.descripcion()), estado, fechaInicio, fechaEstimadaFin,
                porcentajeAvance, montoPresupuestado, costosEstimados, pagosPrevistos, cobrosPrevistos,
                vacioANull(cruda.observaciones()), nombresProveedores, idsProveedores, errores);
    }

    private List<String> revalidar(EtapaImportFilaDto fila) {
        List<String> errores = new ArrayList<>();
        if (fila.nombre() == null || fila.nombre().isBlank()) {
            errores.add("El nombre es obligatorio");
        }
        if (fila.estado() != null) {
            try {
                Etapa.EstadoEtapa.valueOf(fila.estado());
            } catch (IllegalArgumentException e) {
                errores.add("Estado inválido: " + fila.estado());
            }
        }
        if (fila.porcentajeAvance() != null && (fila.porcentajeAvance() < 0 || fila.porcentajeAvance() > 100)) {
            errores.add("El % de avance debe estar entre 0 y 100");
        }
        if (negativo(fila.montoPresupuestado()) || negativo(fila.costosEstimados())
                || negativo(fila.pagosPrevistos()) || negativo(fila.cobrosPrevistos())) {
            errores.add("Los importes no pueden ser negativos");
        }
        if (fila.proveedoresIds() != null && !fila.proveedoresIds().isEmpty()) {
            long encontrados = proveedorRepo.findAllById(fila.proveedoresIds()).size();
            if (encontrados != fila.proveedoresIds().size()) {
                errores.add("Hay proveedores referenciados que ya no existen");
            }
        }
        return errores;
    }

    private boolean negativo(BigDecimal valor) {
        return valor != null && valor.signum() < 0;
    }

    private String normalizarEstado(String crudo, List<String> errores) {
        String limpio = vacioANull(crudo);
        if (limpio == null) {
            return Etapa.EstadoEtapa.PENDIENTE.name();
        }
        String candidato = limpio.trim().toUpperCase().replace(' ', '_');
        boolean valido = Arrays.stream(Etapa.EstadoEtapa.values()).anyMatch(e -> e.name().equals(candidato));
        if (!valido) {
            errores.add("Estado inválido: " + crudo);
            return null;
        }
        return candidato;
    }

    private LocalDate parsearFecha(String crudo, String etiqueta, List<String> errores) {
        String limpio = vacioANull(crudo);
        if (limpio == null) {
            return null;
        }
        try {
            return LocalDate.parse(limpio, FORMATO_FECHA);
        } catch (DateTimeParseException e) {
            errores.add("Formato de " + etiqueta + " inválido (esperado dd/MM/yyyy): " + crudo);
            return null;
        }
    }

    private Integer parsearPorcentaje(String crudo, List<String> errores) {
        String limpio = vacioANull(crudo);
        if (limpio == null) {
            return null;
        }
        try {
            int valor = Integer.parseInt(limpio.replace("%", "").trim());
            if (valor < 0 || valor > 100) {
                errores.add("El % de avance debe estar entre 0 y 100");
                return null;
            }
            return valor;
        } catch (NumberFormatException e) {
            errores.add("% de avance inválido: " + crudo);
            return null;
        }
    }

    private BigDecimal parsearImporte(String crudo, String etiqueta, List<String> errores) {
        String limpio = vacioANull(crudo);
        if (limpio == null) {
            return null;
        }
        try {
            String normalizado = normalizarImporte(limpio);
            BigDecimal valor = new BigDecimal(normalizado);
            if (valor.signum() < 0) {
                errores.add("El " + etiqueta + " no puede ser negativo");
            }
            return valor;
        } catch (NumberFormatException e) {
            errores.add("Formato de " + etiqueta + " inválido: " + crudo);
            return null;
        }
    }

    /** Acepta tanto "1.234,56" (es-AR) como "1234.56" (en-US) o "1234,56". */
    private String normalizarImporte(String valor) {
        String limpio = valor.replace(" ", "");
        boolean tieneComa = limpio.contains(",");
        boolean tienePunto = limpio.contains(".");
        if (tieneComa && tienePunto) {
            return limpio.replace(".", "").replace(",", ".");
        }
        if (tieneComa) {
            return limpio.replace(",", ".");
        }
        return limpio;
    }

    private String vacioANull(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}
