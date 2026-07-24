package com.montanaritech.contable.impuestos.iibb;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.common.reporte.ContextoReporte;
import com.montanaritech.contable.common.reporte.ReportExportService;
import com.montanaritech.contable.impuestos.iibb.dto.LiquidacionIibbDtos.AgregarComponenteRequest;
import com.montanaritech.contable.impuestos.iibb.dto.LiquidacionIibbDtos.AjustarComponenteRequest;
import com.montanaritech.contable.impuestos.iibb.dto.LiquidacionIibbDtos.AnularRequest;
import com.montanaritech.contable.impuestos.iibb.dto.LiquidacionIibbDtos.CrearRequest;
import com.montanaritech.contable.impuestos.iibb.dto.LiquidacionIibbDtos.EditarJurisdiccionRequest;
import com.montanaritech.contable.impuestos.iibb.dto.LiquidacionIibbDtos.LiquidacionResponse;
import com.montanaritech.contable.impuestos.iibb.dto.LiquidacionIibbDtos.PrevisualizacionResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/** Liquidaciones de IIBB (F7.6, molde PL-3 — mismo criterio que {@code CuentaPorCobrarController} de F4.5). */
@RestController
@RequestMapping("/api/v1/impuestos/liquidaciones-iibb")
@RequiredArgsConstructor
public class LiquidacionIibbController {

    private static final List<String> COLUMNAS = List.of("Período", "Estado", "A pagar", "A favor", "Asiento");

    private final LiquidacionIibbService service;
    private final LiquidacionIibbMapper mapper;
    private final ReportExportService reportExportService;

    @GetMapping
    public Page<LiquidacionResponse> listar(@RequestParam(required = false) Integer anio,
                                            @RequestParam(required = false) EstadoDocumento estado,
                                            @PageableDefault(size = 20) Pageable pageable) {
        return service.listar(anio, estado, pageable).map(l -> mapper.aResponse(l, List.of()));
    }

    @GetMapping("/exportar/excel")
    public ResponseEntity<StreamingResponseBody> exportarExcel(
            @RequestParam(required = false) Integer anio, @RequestParam(required = false) EstadoDocumento estado) {
        List<List<Object>> filas = aFilas(service.listar(anio, estado, Pageable.unpaged()));
        ContextoReporte contexto = contexto(anio, estado);
        StreamingResponseBody cuerpo = out -> reportExportService.exportarExcel(contexto, COLUMNAS, filas, out);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"liquidaciones-iibb.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(cuerpo);
    }

    @GetMapping("/exportar/pdf")
    public ResponseEntity<StreamingResponseBody> exportarPdf(
            @RequestParam(required = false) Integer anio, @RequestParam(required = false) EstadoDocumento estado) {
        List<List<Object>> filas = aFilas(service.listar(anio, estado, Pageable.unpaged()));
        ContextoReporte contexto = contexto(anio, estado);
        StreamingResponseBody cuerpo = out -> {
            try {
                reportExportService.exportarPdf(contexto, COLUMNAS, filas, out);
            } catch (Exception e) {
                throw new IOException("No se pudo generar el PDF", e);
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"liquidaciones-iibb.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(cuerpo);
    }

    private List<List<Object>> aFilas(Page<LiquidacionIibb> liquidaciones) {
        return liquidaciones.map(l -> mapper.aResponse(l, List.of())).stream()
                .<List<Object>>map(l -> List.of(
                        l.mes() + "/" + l.anio(),
                        l.estado(),
                        l.saldoAPagarTotal(),
                        l.saldoAFavorTotal(),
                        l.asientoNumero() == null ? "" : l.asientoNumero()))
                .toList();
    }

    private ContextoReporte contexto(Integer anio, EstadoDocumento estado) {
        return ContextoReporte.de("Liquidaciones de IIBB",
                anio == null ? null : "Año: " + anio,
                estado == null ? null : "Estado: " + estado);
    }

    @GetMapping("/{id}")
    public LiquidacionResponse obtener(@PathVariable Long id) {
        return mapper.aResponse(service.obtener(id), List.of());
    }

    @GetMapping("/previsualizar")
    public PrevisualizacionResponse previsualizar(@RequestParam int anio, @RequestParam int mes) {
        return mapper.aPrevisualizacion(service.previsualizar(anio, mes));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public LiquidacionResponse crear(@Valid @RequestBody CrearRequest req) {
        return mapper.aResponse(service.crearBorrador(req), List.of());
    }

    @PostMapping("/{id}/recalcular")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public LiquidacionResponse recalcular(@PathVariable Long id) {
        return mapper.aResponse(service.recalcular(id), List.of());
    }

    @PatchMapping("/{id}/jurisdicciones/{jurLiqId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public LiquidacionResponse editarJurisdiccion(@PathVariable Long id, @PathVariable Long jurLiqId,
                                                  @Valid @RequestBody EditarJurisdiccionRequest req) {
        return mapper.aResponse(service.editarJurisdiccion(id, jurLiqId, req), List.of());
    }

    @PatchMapping("/{id}/jurisdicciones/{jurLiqId}/componentes/{componenteId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public LiquidacionResponse ajustarComponente(@PathVariable Long id, @PathVariable Long jurLiqId,
                                                 @PathVariable Long componenteId,
                                                 @Valid @RequestBody AjustarComponenteRequest req) {
        return mapper.aResponse(service.ajustarComponente(id, jurLiqId, componenteId, req), List.of());
    }

    @PostMapping("/{id}/jurisdicciones/{jurLiqId}/componentes")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public LiquidacionResponse agregarComponente(@PathVariable Long id, @PathVariable Long jurLiqId,
                                                 @Valid @RequestBody AgregarComponenteRequest req) {
        return mapper.aResponse(service.agregarComponente(id, jurLiqId, req), List.of());
    }

    @DeleteMapping("/{id}/jurisdicciones/{jurLiqId}/componentes/{componenteId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public LiquidacionResponse eliminarComponente(@PathVariable Long id, @PathVariable Long jurLiqId,
                                                  @PathVariable Long componenteId) {
        return mapper.aResponse(service.eliminarComponente(id, jurLiqId, componenteId), List.of());
    }

    @PatchMapping("/{id}/confirmar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public LiquidacionResponse confirmar(@PathVariable Long id) {
        return mapper.aResponse(service.confirmar(id), List.of());
    }

    @PatchMapping("/{id}/anular")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public LiquidacionResponse anular(@PathVariable Long id, @Valid @RequestBody AnularRequest req) {
        return mapper.aResponse(service.anular(id, req.motivo()), List.of());
    }
}
