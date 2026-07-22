package com.montanaritech.contable.bancos.importacion;

import com.montanaritech.contable.bancos.importacion.dto.FilaImportacionConfirmarRequest;
import com.montanaritech.contable.bancos.importacion.dto.FilaImportacionPreviewResponse;
import com.montanaritech.contable.bancos.importacion.dto.FilaImportacionResultadoResponse;
import com.montanaritech.contable.bancos.movimientobancario.OrigenImportacionMovimiento;
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

/** Importación de resúmenes bancarios/tarjeta (F5.2): Galicia, Mercado Pago, tarjeta VISA. */
@RestController
@RequestMapping("/api/v1/importacion-movimientos-bancarios")
@RequiredArgsConstructor
@Tag(name = "ImportacionMovimientoBancario")
public class ImportacionMovimientoBancarioController {

    private final ImportacionMovimientoBancarioService service;

    @PostMapping(value = "/{origen}/previsualizar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public List<FilaImportacionPreviewResponse> previsualizar(@PathVariable OrigenImportacionMovimiento origen,
            @RequestParam Long cuentaBancariaId, @RequestParam("archivo") MultipartFile archivo) {
        return service.previsualizar(origen, cuentaBancariaId, leerBytes(archivo));
    }

    @PostMapping("/{origen}/confirmar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public List<FilaImportacionResultadoResponse> confirmar(@PathVariable OrigenImportacionMovimiento origen,
            @RequestParam Long cuentaBancariaId, @RequestParam(required = false) BigDecimal tipoCambioUsd,
            @Valid @RequestBody List<@Valid FilaImportacionConfirmarRequest> filas) {
        return service.confirmar(origen, cuentaBancariaId, tipoCambioUsd, filas);
    }

    private byte[] leerBytes(MultipartFile archivo) {
        try {
            return archivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo " + archivo.getOriginalFilename(), e);
        }
    }
}
