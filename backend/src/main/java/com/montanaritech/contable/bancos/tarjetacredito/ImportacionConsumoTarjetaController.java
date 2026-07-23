package com.montanaritech.contable.bancos.tarjetacredito;

import com.montanaritech.contable.bancos.tarjetacredito.dto.ConsumoImportacionConfirmarRequest;
import com.montanaritech.contable.bancos.tarjetacredito.dto.ConsumoImportacionPreviewResponse;
import com.montanaritech.contable.bancos.tarjetacredito.dto.ConsumoImportacionResultadoResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Importación del resumen de tarjeta (F5.4): reusa ParserTarjeta (F5.2), pero produce ConsumoTarjeta en vez de MovimientoBancario. */
@RestController
@RequestMapping("/api/v1/tarjetas-credito/{tarjetaCreditoId}/importacion-consumos")
@RequiredArgsConstructor
@Tag(name = "ImportacionConsumoTarjeta")
public class ImportacionConsumoTarjetaController {

    private final ImportacionConsumoTarjetaService service;

    @PostMapping(value = "/previsualizar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public List<ConsumoImportacionPreviewResponse> previsualizar(@PathVariable Long tarjetaCreditoId,
            @RequestParam("archivo") MultipartFile archivo) {
        return service.previsualizar(tarjetaCreditoId, leerBytes(archivo));
    }

    @PostMapping("/confirmar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public List<ConsumoImportacionResultadoResponse> confirmar(@PathVariable Long tarjetaCreditoId,
            @RequestParam(required = false) BigDecimal tipoCambioUsd,
            @Valid @RequestBody List<@Valid ConsumoImportacionConfirmarRequest> filas) {
        return service.confirmar(tarjetaCreditoId, tipoCambioUsd, filas);
    }

    private byte[] leerBytes(MultipartFile archivo) {
        try {
            return archivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo " + archivo.getOriginalFilename(), e);
        }
    }
}
