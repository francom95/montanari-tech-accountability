package com.montanaritech.contable.maestros.proyecto.importacion;

import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.importacion.ImportUtils;
import com.montanaritech.contable.maestros.cliente.ClienteRepository;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.proyecto.ProyectoMapper;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import com.montanaritech.contable.maestros.proyecto.ProyectoService;
import com.montanaritech.contable.maestros.proyecto.dto.CuotaRequest;
import com.montanaritech.contable.maestros.proyecto.dto.ProyectoCrearRequest;
import com.montanaritech.contable.maestros.proyecto.dto.ProyectoResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Previsualización/confirmación de la importación de proyectos desde la hoja
 * "Clientes" (F10.2, molde de {@code EtapaImportService}: FK por nombre
 * resuelta ya en previsualizar, revalidada en confirmar).
 *
 * Fila cuyo cliente no se encuentra (por nombre) se rechaza — el cliente
 * pudo no migrarse por falta de CUIT (decisión de F10.1), efecto en cascada
 * esperado, no un error propio de esta fila.
 */
@Service
@RequiredArgsConstructor
public class ProyectoImportService {

    private final List<ProyectoImportParser> parsers;
    private final ClienteRepository clienteRepo;
    private final ProyectoRepository proyectoRepo;
    private final ProyectoService proyectoService;
    private final ProyectoMapper proyectoMapper;

    public List<ProyectoImportFilaDto> previsualizar(MultipartFile archivo) {
        String nombreArchivo = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "";
        ProyectoImportParser parser = parsers.stream()
                .filter(p -> p.soporta(nombreArchivo))
                .findFirst()
                .orElseThrow(() -> new NegocioException("FORMATO_NO_SOPORTADO", "Formato de archivo no soportado. Usá .xlsx o .csv"));

        List<ProyectoImportFilaCruda> crudas;
        try (InputStream in = archivo.getInputStream()) {
            crudas = parser.parsear(in);
        } catch (IOException e) {
            throw new NegocioException("ARCHIVO_ILEGIBLE", "No se pudo leer el archivo: " + e.getMessage());
        }

        return crudas.stream().map(this::validar).toList();
    }

    @Transactional
    public ProyectoImportResultado confirmar(Long monedaIdUsd, List<ProyectoImportFilaDto> filas) {
        List<ProyectoResponse> creadas = new ArrayList<>();
        List<ProyectoImportFilaDto> rechazadas = new ArrayList<>();
        int yaExistian = 0;

        for (ProyectoImportFilaDto fila : filas) {
            List<String> errores = revalidar(fila);
            if (!errores.isEmpty()) {
                rechazadas.add(new ProyectoImportFilaDto(fila.fila(), fila.clienteNombre(), fila.proyectoNombre(),
                        fila.pais(), fila.tipoProyecto(), fila.estado(), fila.montoTotal(), fila.cuotas(), fila.comentarios(), errores));
                continue;
            }

            Long clienteId = clienteRepo.findByNombreIgnoreCase(fila.clienteNombre()).orElseThrow().getId();
            if (proyectoRepo.findByNombreIgnoreCaseAndClienteId(fila.proyectoNombre(), clienteId).isPresent()) {
                yaExistian++;
                continue;
            }

            List<CuotaRequest> cuotas = fila.cuotas().stream().map(importe -> new CuotaRequest(null, importe)).toList();
            ProyectoCrearRequest req = new ProyectoCrearRequest(
                    fila.proyectoNombre(), clienteId, null, fila.pais(), fila.tipoProyecto(), fila.estado(),
                    monedaIdUsd, fila.montoTotal(), cuotas.isEmpty() ? null : cuotas.size(), fila.comentarios(),
                    null, null, null, null, null, cuotas);
            Proyecto creado = proyectoService.crear(req);
            creadas.add(proyectoMapper.aResponse(creado));
        }

        return new ProyectoImportResultado(creadas, rechazadas, yaExistian);
    }

    private ProyectoImportFilaDto validar(ProyectoImportFilaCruda cruda) {
        List<String> errores = new ArrayList<>();

        String compuesto = ImportUtils.vacioANull(cruda.proyectoCompuesto());
        if (compuesto == null) {
            errores.add("La columna Proyecto (texto compuesto Cliente-Proyecto) es obligatoria");
        }
        String clienteNombre = compuesto != null ? extraerClienteNombre(compuesto) : null;
        String proyectoNombre = compuesto;

        if (clienteNombre != null && clienteRepo.findByNombreIgnoreCase(clienteNombre).isEmpty()) {
            errores.add("Cliente '" + clienteNombre + "' no encontrado (no migrado, o rechazado por falta de CUIT — completar Cliente antes de reintentar)");
        }

        String pais = ImportUtils.vacioANull(cruda.pais());
        String tipoProyecto = pais != null && pais.equalsIgnoreCase("Argentina") ? "ARGENTINA" : "EXTERIOR";

        BigDecimal montoTotal = ImportUtils.parsearImporte(cruda.montoTotalUsd(), "monto total", errores);
        if (montoTotal == null) {
            montoTotal = BigDecimal.ZERO;
        }

        List<BigDecimal> cuotas = new ArrayList<>();
        for (String pago : List.of(cruda.pago1(), cruda.pago2(), cruda.pago3(), cruda.pago4(),
                cruda.pago5(), cruda.pago6(), cruda.pago7(), cruda.pago8())) {
            BigDecimal importe = ImportUtils.parsearImporte(pago, "cuota", errores);
            if (importe != null) {
                cuotas.add(importe);
            }
        }

        String responsables = ImportUtils.vacioANull(cruda.responsables());
        String comentariosOriginales = ImportUtils.vacioANull(cruda.comentarios());
        String comentarios = responsables != null
                ? (comentariosOriginales != null ? comentariosOriginales + " | " : "") + "Responsable(s) original: " + responsables
                : comentariosOriginales;

        String estado = heuristicaEstado(montoTotal, comentariosOriginales);

        return new ProyectoImportFilaDto(cruda.numeroFila(), clienteNombre, proyectoNombre, pais, tipoProyecto,
                estado, montoTotal, cuotas, comentarios, errores);
    }

    private List<String> revalidar(ProyectoImportFilaDto fila) {
        List<String> errores = new ArrayList<>();
        if (fila.clienteNombre() == null || fila.proyectoNombre() == null) {
            errores.add("La columna Proyecto (texto compuesto Cliente-Proyecto) es obligatoria");
        } else if (clienteRepo.findByNombreIgnoreCase(fila.clienteNombre()).isEmpty()) {
            errores.add("Cliente '" + fila.clienteNombre() + "' no encontrado (no migrado, o rechazado por falta de CUIT — completar Cliente antes de reintentar)");
        }
        if (fila.montoTotal() != null && fila.montoTotal().signum() < 0) {
            errores.add("El monto total no puede ser negativo");
        }
        return errores;
    }

    private String extraerClienteNombre(String compuesto) {
        int idxGuion = compuesto.indexOf(" - ");
        int idxParentesis = compuesto.indexOf(" (");
        int idx;
        if (idxGuion >= 0 && idxParentesis >= 0) {
            idx = Math.min(idxGuion, idxParentesis);
        } else if (idxGuion >= 0) {
            idx = idxGuion;
        } else {
            idx = idxParentesis;
        }
        return idx > 0 ? compuesto.substring(0, idx).trim() : compuesto.trim();
    }

    private String heuristicaEstado(BigDecimal montoTotal, String comentarios) {
        if (comentarios != null && comentarios.toUpperCase().contains("FINALIZADO")) {
            return "FINALIZADO";
        }
        if (montoTotal.signum() == 0 && comentarios == null) {
            return "PROSPECTO";
        }
        return "EN_CURSO";
    }
}
