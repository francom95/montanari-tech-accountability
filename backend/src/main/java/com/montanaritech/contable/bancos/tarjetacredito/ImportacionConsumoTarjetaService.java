package com.montanaritech.contable.bancos.tarjetacredito;

import com.montanaritech.contable.bancos.importacion.MovimientoParseado;
import com.montanaritech.contable.bancos.importacion.ParserTarjeta;
import com.montanaritech.contable.bancos.tarjetacredito.dto.ConsumoImportacionConfirmarRequest;
import com.montanaritech.contable.bancos.tarjetacredito.dto.ConsumoImportacionPreviewResponse;
import com.montanaritech.contable.bancos.tarjetacredito.dto.ConsumoImportacionResultadoResponse;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.common.saldo.RecalculoSaldoService;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import com.montanaritech.contable.maestros.tarjetacredito.TarjetaCredito;
import com.montanaritech.contable.maestros.tarjetacredito.TarjetaCreditoRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Importación del resumen de tarjeta (F5.4): reusa {@link ParserTarjeta}
 * (F5.2) tal cual — mismo extractor PDFBox, sin duplicar nada — pero en vez
 * de alimentar la bandeja de movimientos bancarios (F5.1, para el bloque
 * CONSOLIDADO que sí toca la cuenta bancaria), acá cada fila se convierte en
 * un {@code ConsumoTarjeta} sin clasificar (F5.4 §1/§2): el detalle de
 * consumos que F5.2 dejó explícitamente fuera de su alcance.
 */
@Service
@RequiredArgsConstructor
public class ImportacionConsumoTarjetaService {

    private static final String MONEDA_LIBRO = "ARS";

    private final ParserTarjeta parser;
    private final TarjetaCreditoRepository tarjetaCreditoRepository;
    private final ConsumoTarjetaRepository consumoTarjetaRepository;
    private final MonedaRepository monedaRepository;
    private final RecalculoSaldoService recalculoSaldoService;

    @Transactional(readOnly = true)
    public List<ConsumoImportacionPreviewResponse> previsualizar(Long tarjetaCreditoId, byte[] contenido) {
        resolverTarjeta(tarjetaCreditoId); // valida que exista antes de gastar tiempo parseando

        List<MovimientoParseado> filas = parser.parsear(contenido);
        return filas.stream()
                .map(fila -> {
                    String hash = calcularHash(tarjetaCreditoId, fila);
                    boolean duplicado = consumoTarjetaRepository.existsByTarjetaCredito_IdAndHashImportacion(tarjetaCreditoId, hash);
                    return new ConsumoImportacionPreviewResponse(fila.fecha(), fila.descripcion(), fila.importe(),
                            fila.monedaCodigo(), fila.referencia(), duplicado, hash);
                })
                .toList();
    }

    @Transactional
    public List<ConsumoImportacionResultadoResponse> confirmar(Long tarjetaCreditoId, BigDecimal tipoCambioUsd,
            List<ConsumoImportacionConfirmarRequest> filas) {
        TarjetaCredito tarjeta = resolverTarjeta(tarjetaCreditoId);

        List<ConsumoImportacionResultadoResponse> resultados = filas.stream()
                .map(fila -> confirmarFila(tarjeta, tipoCambioUsd, fila))
                .toList();
        recalculoSaldoService.recalcular(tarjeta);
        return resultados;
    }

    private ConsumoImportacionResultadoResponse confirmarFila(TarjetaCredito tarjeta, BigDecimal tipoCambioUsd,
            ConsumoImportacionConfirmarRequest fila) {
        try {
            if (consumoTarjetaRepository.existsByTarjetaCredito_IdAndHashImportacion(tarjeta.getId(), fila.hash())) {
                return new ConsumoImportacionResultadoResponse(fila.descripcion(), "DUPLICADO", null, null);
            }
            if (fila.importe().compareTo(BigDecimal.ZERO) == 0) {
                throw new NegocioException("IMPORTE_CERO", "El importe del consumo no puede ser cero");
            }

            Moneda moneda = resolverMoneda(fila.monedaCodigo());
            BigDecimal tipoCambio;
            BigDecimal importeArs;
            if (MONEDA_LIBRO.equals(fila.monedaCodigo())) {
                tipoCambio = BigDecimal.ONE;
                importeArs = fila.importe();
            } else {
                if (tipoCambioUsd == null) {
                    throw new NegocioException("TC_REQUERIDO", "Falta el tipo de cambio para importar filas en " + fila.monedaCodigo());
                }
                tipoCambio = tipoCambioUsd;
                importeArs = fila.importe().multiply(tipoCambio).setScale(2, java.math.RoundingMode.HALF_UP);
            }

            ConsumoTarjeta consumo = new ConsumoTarjeta();
            consumo.setTarjetaCredito(tarjeta);
            consumo.setFecha(fila.fecha());
            consumo.setDescripcion(fila.descripcion());
            consumo.setReferencia(fila.referencia());
            consumo.setImporte(fila.importe());
            consumo.setMoneda(moneda);
            consumo.setTipoCambio(tipoCambio);
            consumo.setImporteArs(importeArs);
            consumo.setHashImportacion(fila.hash());
            ConsumoTarjeta guardado = consumoTarjetaRepository.save(consumo);

            return new ConsumoImportacionResultadoResponse(fila.descripcion(), "IMPORTADO", null, guardado.getId());
        } catch (NegocioException e) {
            return new ConsumoImportacionResultadoResponse(fila.descripcion(), "ERROR", e.getMessage(), null);
        }
    }

    private String calcularHash(Long tarjetaCreditoId, MovimientoParseado fila) {
        String base = tarjetaCreditoId + "|" + fila.fecha() + "|" + fila.importe().stripTrailingZeros().toPlainString()
                + "|" + fila.descripcion() + "|" + fila.referencia();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(base.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    private TarjetaCredito resolverTarjeta(Long id) {
        return tarjetaCreditoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tarjeta de crédito " + id + " no encontrada"));
    }

    private Moneda resolverMoneda(String codigo) {
        return monedaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Moneda " + codigo + " no encontrada"));
    }
}
