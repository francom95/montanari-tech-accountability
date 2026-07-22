package com.montanaritech.contable.bancos.importacion;

import com.montanaritech.contable.bancos.importacion.dto.FilaImportacionConfirmarRequest;
import com.montanaritech.contable.bancos.importacion.dto.FilaImportacionPreviewResponse;
import com.montanaritech.contable.bancos.importacion.dto.FilaImportacionResultadoResponse;
import com.montanaritech.contable.bancos.movimientobancario.MovimientoBancarioRepository;
import com.montanaritech.contable.bancos.movimientobancario.MovimientoBancarioService;
import com.montanaritech.contable.bancos.movimientobancario.OrigenImportacionMovimiento;
import com.montanaritech.contable.bancos.movimientobancario.dto.CrearMovimientoBancarioRequest;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancaria;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancariaRepository;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Importación de resúmenes bancarios (F5.2): cada {@link ResumenParser} solo
 * normaliza el archivo, nunca persiste — acá se resuelve la moneda faltante
 * (Galicia no la declara: se usa la de la cuenta bancaria destino), se
 * detectan duplicados (hash de cuenta+fecha+importe+descripción+referencia,
 * regla de negocio del paso) y recién en {@code confirmar} se crean los
 * {@code MovimientoBancario} vía {@link MovimientoBancarioService#crear},
 * que los deja PENDIENTE — igual que la carga manual de F5.1, nunca
 * impactan la contabilidad automáticamente.
 */
@Service
public class ImportacionMovimientoBancarioService {

    private static final String MONEDA_LIBRO = "ARS";

    private final MovimientoBancarioRepository movimientoBancarioRepo;
    private final MovimientoBancarioService movimientoBancarioService;
    private final CuentaBancariaRepository cuentaBancariaRepo;
    private final MonedaRepository monedaRepo;
    private final Map<OrigenImportacionMovimiento, ResumenParser> parsers;

    public ImportacionMovimientoBancarioService(MovimientoBancarioRepository movimientoBancarioRepo,
            MovimientoBancarioService movimientoBancarioService, CuentaBancariaRepository cuentaBancariaRepo,
            MonedaRepository monedaRepo, List<ResumenParser> parsers) {
        this.movimientoBancarioRepo = movimientoBancarioRepo;
        this.movimientoBancarioService = movimientoBancarioService;
        this.cuentaBancariaRepo = cuentaBancariaRepo;
        this.monedaRepo = monedaRepo;
        this.parsers = parsers.stream().collect(java.util.stream.Collectors.toMap(ResumenParser::origen, Function.identity()));
    }

    @Transactional(readOnly = true)
    public List<FilaImportacionPreviewResponse> previsualizar(OrigenImportacionMovimiento origen, Long cuentaBancariaId, byte[] contenido) {
        CuentaBancaria cuentaBancaria = resolverCuentaBancaria(cuentaBancariaId);
        List<MovimientoParseado> filas = resolverParser(origen).parsear(contenido);

        return filas.stream()
                .map(fila -> {
                    String monedaCodigo = fila.monedaCodigo() != null ? fila.monedaCodigo() : cuentaBancaria.getMoneda().getCodigo();
                    String hash = calcularHash(cuentaBancariaId, fila);
                    boolean duplicado = movimientoBancarioRepo.existsByCuentaBancaria_IdAndHashImportacion(cuentaBancariaId, hash);
                    return new FilaImportacionPreviewResponse(fila.fecha(), fila.descripcion(), fila.importe(), monedaCodigo,
                            fila.referencia(), duplicado, hash);
                })
                .toList();
    }

    @Transactional
    public List<FilaImportacionResultadoResponse> confirmar(OrigenImportacionMovimiento origen, Long cuentaBancariaId,
            BigDecimal tipoCambioUsd, List<FilaImportacionConfirmarRequest> filas) {
        CuentaBancaria cuentaBancaria = resolverCuentaBancaria(cuentaBancariaId);

        return filas.stream()
                .map(fila -> confirmarFila(origen, cuentaBancaria, tipoCambioUsd, fila))
                .toList();
    }

    private FilaImportacionResultadoResponse confirmarFila(OrigenImportacionMovimiento origen, CuentaBancaria cuentaBancaria,
            BigDecimal tipoCambioUsd, FilaImportacionConfirmarRequest fila) {
        try {
            if (movimientoBancarioRepo.existsByCuentaBancaria_IdAndHashImportacion(cuentaBancaria.getId(), fila.hash())) {
                return new FilaImportacionResultadoResponse(fila.descripcion(), "DUPLICADO", null, null);
            }

            String monedaCodigo = fila.monedaCodigo() != null ? fila.monedaCodigo() : cuentaBancaria.getMoneda().getCodigo();
            Moneda moneda = resolverMoneda(monedaCodigo);
            BigDecimal tipoCambio;
            if (MONEDA_LIBRO.equals(monedaCodigo)) {
                tipoCambio = BigDecimal.ONE;
            } else {
                if (tipoCambioUsd == null) {
                    throw new NegocioException("TC_REQUERIDO",
                            "Falta el tipo de cambio para importar filas en " + monedaCodigo);
                }
                tipoCambio = tipoCambioUsd;
            }

            CrearMovimientoBancarioRequest req = new CrearMovimientoBancarioRequest(cuentaBancaria.getId(), fila.fecha(),
                    fila.descripcion(), fila.importe(), moneda.getId(), tipoCambio, fila.referencia(), null, null,
                    origen, fila.hash());
            var creado = movimientoBancarioService.crear(req);

            return new FilaImportacionResultadoResponse(fila.descripcion(), "IMPORTADO", null, creado.getId());
        } catch (NegocioException e) {
            return new FilaImportacionResultadoResponse(fila.descripcion(), "ERROR", e.getMessage(), null);
        }
    }

    private String calcularHash(Long cuentaBancariaId, MovimientoParseado fila) {
        String base = cuentaBancariaId + "|" + fila.fecha() + "|" + fila.importe().stripTrailingZeros().toPlainString()
                + "|" + fila.descripcion() + "|" + fila.referencia();
        return sha256Hex(base);
    }

    private String sha256Hex(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    private ResumenParser resolverParser(OrigenImportacionMovimiento origen) {
        ResumenParser parser = parsers.get(origen);
        if (parser == null) {
            throw new NegocioException("ORIGEN_SIN_PARSER", "No hay un parser de importación para el origen " + origen);
        }
        return parser;
    }

    private CuentaBancaria resolverCuentaBancaria(Long id) {
        return cuentaBancariaRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta bancaria " + id + " no encontrada"));
    }

    private Moneda resolverMoneda(String codigo) {
        return monedaRepo.findByCodigo(codigo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Moneda " + codigo + " no encontrada"));
    }
}
