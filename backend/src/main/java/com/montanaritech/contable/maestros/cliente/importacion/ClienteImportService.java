package com.montanaritech.contable.maestros.cliente.importacion;

import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.importacion.ImportUtils;
import com.montanaritech.contable.maestros.cliente.Cliente;
import com.montanaritech.contable.maestros.cliente.ClienteMapper;
import com.montanaritech.contable.maestros.cliente.ClienteRepository;
import com.montanaritech.contable.maestros.cliente.ClienteService;
import com.montanaritech.contable.maestros.cliente.dto.ClienteCrearRequest;
import com.montanaritech.contable.maestros.cliente.dto.ClienteResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Previsualización (parsear + validar sin persistir) y confirmación
 * (persistir solo las filas sin error) de la importación de clientes desde
 * la hoja "Base de datos - Clientes" (F10.2, molde de {@code EtapaImportService} de F2.5).
 *
 * Fila sin CUIT se rechaza (no se migra automáticamente — decisión de F10.1: se completa a mano después).
 */
@Service
@RequiredArgsConstructor
public class ClienteImportService {

    private final List<ClienteImportParser> parsers;
    private final ClienteRepository clienteRepo;
    private final ClienteService clienteService;
    private final ClienteMapper clienteMapper;

    public List<ClienteImportFilaDto> previsualizar(MultipartFile archivo) {
        String nombreArchivo = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "";
        ClienteImportParser parser = parsers.stream()
                .filter(p -> p.soporta(nombreArchivo))
                .findFirst()
                .orElseThrow(() -> new NegocioException("FORMATO_NO_SOPORTADO", "Formato de archivo no soportado. Usá .xlsx o .csv"));

        List<ClienteImportFilaCruda> crudas;
        try (InputStream in = archivo.getInputStream()) {
            crudas = parser.parsear(in);
        } catch (IOException e) {
            throw new NegocioException("ARCHIVO_ILEGIBLE", "No se pudo leer el archivo: " + e.getMessage());
        }

        return crudas.stream().map(this::validar).toList();
    }

    @Transactional
    public ClienteImportResultado confirmar(Long jurisdiccionIdPorDefecto, List<ClienteImportFilaDto> filas) {
        List<ClienteResponse> creadas = new ArrayList<>();
        List<ClienteImportFilaDto> rechazadas = new ArrayList<>();
        int yaExistian = 0;

        for (ClienteImportFilaDto fila : filas) {
            List<String> errores = revalidar(fila);
            if (!errores.isEmpty()) {
                rechazadas.add(new ClienteImportFilaDto(fila.fila(), fila.nombre(), fila.cuit(), errores));
                continue;
            }

            if (clienteRepo.findByNombreIgnoreCase(fila.nombre()).isPresent()) {
                yaExistian++;
                continue;
            }

            ClienteCrearRequest req = new ClienteCrearRequest(
                    fila.nombre(), fila.cuit(), jurisdiccionIdPorDefecto, null, null, null, null);
            Cliente creado = clienteService.crear(req);
            creadas.add(clienteMapper.aResponse(creado));
        }

        return new ClienteImportResultado(creadas, rechazadas, yaExistian);
    }

    private ClienteImportFilaDto validar(ClienteImportFilaCruda cruda) {
        List<String> errores = new ArrayList<>();

        String razonSocial = ImportUtils.vacioANull(cruda.razonSocial());
        String nombreClientes = ImportUtils.vacioANull(cruda.nombreClientes());
        String nombre = razonSocial != null ? razonSocial : nombreClientes;
        if (nombre == null) {
            errores.add("El nombre (Razón Social o Nombre Clientes) es obligatorio");
        }

        String cuit = ImportUtils.vacioANull(cruda.cuit());
        if (cuit == null) {
            errores.add("CUIT no informado — no se migra automáticamente (decisión F10.1), completar a mano después");
        }

        return new ClienteImportFilaDto(cruda.numeroFila(), nombre, cuit, errores);
    }

    private List<String> revalidar(ClienteImportFilaDto fila) {
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
