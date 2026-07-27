package com.montanaritech.contable.contabilidad.asiento.importacion;

import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.common.importacion.ImportUtils;
import com.montanaritech.contable.contabilidad.asiento.Asiento;
import com.montanaritech.contable.contabilidad.asiento.AsientoRepository;
import com.montanaritech.contable.contabilidad.asiento.AsientoService;
import com.montanaritech.contable.contabilidad.asiento.dto.AsientoCrearRequest;
import com.montanaritech.contable.contabilidad.asiento.dto.AsientoLineaRequest;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContableRepository;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Previsualización/confirmación de la importación del Libro Diario (F10.2,
 * el núcleo de la migración) — molde de {@code ClienteImportService}
 * adaptado a que cada "entidad" es un grupo de líneas (un {@code Asiento}),
 * no una fila suelta.
 *
 * <p><b>Deliberadamente sin {@code @Transactional} a nivel de método</b>
 * (a diferencia de los otros 7 importadores de F10.2): cada asiento se crea
 * y confirma con sus propias transacciones de {@link AsientoService}
 * (propagación REQUIRED). Si se envolviera {@code confirmar()} acá en una
 * única transacción, una excepción de {@code AsientoService.confirmar}
 * (ej. asiento desbalanceado) marcaría esa misma transacción como
 * rollback-only — y aunque el catch de abajo la atrape y siga con el
 * siguiente grupo, TODOS los asientos ya confirmados en la misma corrida se
 * perderían al hacer rollback al final. Sin el wrapper, cada grupo
 * commitea independientemente: uno malo no arrastra a los demás.
 */
@Service
@RequiredArgsConstructor
public class AsientoImportService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String PREFIJO_DESCRIPCION = "Importación histórica — Asiento Excel N° ";

    private final AsientoImportParser parser;
    private final CuentaContableRepository cuentaContableRepo;
    private final AsientoRepository asientoRepo;
    private final AsientoService asientoService;

    public List<AsientoImportFilaDto> previsualizar(MultipartFile archivo) {
        List<AsientoImportFilaCruda> crudas;
        try (InputStream in = archivo.getInputStream()) {
            crudas = parser.parsear(in);
        } catch (IOException e) {
            throw new NegocioException("ARCHIVO_ILEGIBLE", "No se pudo leer el archivo: " + e.getMessage());
        }

        return crudas.stream().map(this::validar).toList();
    }

    public AsientoImportResultado confirmar(Long monedaIdArs, List<AsientoImportFilaDto> filas) {
        LinkedHashMap<String, List<AsientoImportFilaDto>> grupos = new LinkedHashMap<>();
        for (AsientoImportFilaDto f : filas) {
            grupos.computeIfAbsent(f.numeroAsientoOriginal(), k -> new ArrayList<>()).add(f);
        }

        List<Map.Entry<String, List<AsientoImportFilaDto>>> gruposOrdenados = grupos.entrySet().stream()
                .sorted(Comparator.comparing(e -> Objects.requireNonNullElse(fechaDelGrupo(e.getValue()), LocalDate.MAX)))
                .toList();

        List<AsientoImportResultado.AsientoImportCreadoDto> creados = new ArrayList<>();
        List<AsientoImportFilaDto> rechazadas = new ArrayList<>();
        int yaExistian = 0;

        for (Map.Entry<String, List<AsientoImportFilaDto>> entry : gruposOrdenados) {
            String numeroOriginal = entry.getKey();
            List<AsientoImportFilaDto> lineas = entry.getValue();

            List<String> erroresDeGrupo = new ArrayList<>();
            for (AsientoImportFilaDto l : lineas) {
                erroresDeGrupo.addAll(l.errores());
            }
            LocalDate fecha = fechaDelGrupo(lineas);
            if (fecha == null) {
                erroresDeGrupo.add("No se pudo determinar una fecha para el asiento N° " + numeroOriginal);
            } else if (lineas.stream().anyMatch(l -> l.fecha() != null && !l.fecha().equals(fecha))) {
                erroresDeGrupo.add("Fechas inconsistentes entre líneas del mismo asiento N° " + numeroOriginal);
            }

            if (!erroresDeGrupo.isEmpty()) {
                for (AsientoImportFilaDto l : lineas) {
                    List<String> propios = l.errores().isEmpty()
                            ? List.of("Rechazada: otra línea del asiento N° " + numeroOriginal + " tiene error")
                            : l.errores();
                    rechazadas.add(new AsientoImportFilaDto(l.fila(), l.numeroAsientoOriginal(), l.fecha(),
                            l.cuentaContableId(), l.cuentaContableCodigo(), l.debe(), l.haber(), l.leyenda(), propios));
                }
                continue;
            }

            String descripcion = PREFIJO_DESCRIPCION + numeroOriginal;
            if (asientoRepo.existsByFechaAndDescripcionAndEstado(fecha, descripcion, EstadoDocumento.CONFIRMADO)) {
                yaExistian++;
                continue;
            }

            List<AsientoLineaRequest> lineasReq = lineas.stream()
                    .map(l -> new AsientoLineaRequest(l.cuentaContableId(), l.debe(), l.haber(), monedaIdArs,
                            null, null, l.leyenda(), null, null, null, null, null))
                    .toList();
            AsientoCrearRequest req = new AsientoCrearRequest(fecha, descripcion, null, lineasReq);

            try {
                Asiento borrador = asientoService.crearBorrador(req);
                Asiento confirmado = asientoService.confirmar(borrador.getId());
                creados.add(new AsientoImportResultado.AsientoImportCreadoDto(
                        numeroOriginal, confirmado.getId(), confirmado.getNumero(), fecha, lineas.size()));
            } catch (RuntimeException e) {
                for (AsientoImportFilaDto l : lineas) {
                    rechazadas.add(new AsientoImportFilaDto(l.fila(), l.numeroAsientoOriginal(), l.fecha(),
                            l.cuentaContableId(), l.cuentaContableCodigo(), l.debe(), l.haber(), l.leyenda(),
                            List.of("No se pudo confirmar el asiento N° " + numeroOriginal + ": " + e.getMessage()
                                    + " — revisar si quedó un BORRADOR huérfano para eliminar a mano")));
                }
            }
        }

        return new AsientoImportResultado(creados, rechazadas, yaExistian);
    }

    private LocalDate fechaDelGrupo(List<AsientoImportFilaDto> lineas) {
        return lineas.stream().map(AsientoImportFilaDto::fecha).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private AsientoImportFilaDto validar(AsientoImportFilaCruda cruda) {
        List<String> errores = new ArrayList<>();

        String numeroOriginal = ImportUtils.vacioANull(cruda.numeroAsientoOriginal());
        if (numeroOriginal == null) {
            errores.add("El N° de asiento original es obligatorio (agrupador)");
        }

        LocalDate fecha = ImportUtils.parsearFecha(cruda.fecha(), FORMATO_FECHA, "fecha", errores);
        if (fecha == null && ImportUtils.vacioANull(cruda.fecha()) == null) {
            errores.add("La fecha es obligatoria");
        }

        String codigo = ImportUtils.vacioANull(cruda.codigoDecodificado());
        Long cuentaContableId = null;
        if (codigo == null) {
            errores.add("El código de cuenta es obligatorio");
        } else {
            var cuenta = cuentaContableRepo.findByCodigo(codigo);
            if (cuenta.isEmpty()) {
                errores.add("Código de cuenta '" + codigo + "' no encontrado en el plan de cuentas");
            } else {
                cuentaContableId = cuenta.get().getId();
            }
        }

        BigDecimal debe = parsearImporteODefaultCero(cruda.debe(), "debe", errores);
        BigDecimal haber = parsearImporteODefaultCero(cruda.haber(), "haber", errores);
        if (debe.signum() == 0 && haber.signum() == 0) {
            errores.add("La línea no tiene ni debe ni haber");
        }
        if (debe.signum() != 0 && haber.signum() != 0) {
            errores.add("La línea no puede tener debe y haber a la vez");
        }

        return new AsientoImportFilaDto(cruda.numeroFila(), numeroOriginal, fecha, cuentaContableId, codigo,
                debe, haber, ImportUtils.vacioANull(cruda.leyenda()), errores);
    }

    private BigDecimal parsearImporteODefaultCero(String crudo, String etiqueta, List<String> errores) {
        BigDecimal valor = ImportUtils.parsearImporte(crudo, etiqueta, errores);
        return valor != null ? valor : BigDecimal.ZERO;
    }
}
